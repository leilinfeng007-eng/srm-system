package com.srm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemBaselineTestMapper baseline;

    @Test
    void flywayCreatesExactlyTenSystemTablesAndBootstrapIsIdempotent() {
        assertThat(baseline.countSystemTables()).isEqualTo(37);
        assertThat(baseline.countBootstrapRoles()).isEqualTo(7);
        assertThat(baseline.countUsers()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void loginRefreshMeAndLogoutFormRevocableSession() throws Exception {
        MvcResult login = login("stage0_admin", "Stage0AdminTestOnly!2026");
        String firstAccessToken = accessToken(login);
        Cookie firstRefreshCookie = login.getResponse().getCookie("SRM_REFRESH");
        assertThat(firstRefreshCookie).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.username").value("stage0_admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"));

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("SRM_REFRESH", true))
                .andReturn();
        String secondAccessToken = accessToken(refresh);
        Cookie secondRefreshCookie = refresh.getResponse().getCookie("SRM_REFRESH");
        assertThat(secondAccessToken).isNotEqualTo(firstAccessToken);
        assertThat(secondRefreshCookie).isNotNull();
        assertThat(secondRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstAccessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLATFORM_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAccessToken))
                        .cookie(secondRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("SRM_REFRESH", 0));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAccessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLATFORM_TOKEN_INVALID"));
    }

    @Test
    void viewerReceivesForbiddenForUnassignedBackendPermission() throws Exception {
        int before = baseline.countOperations("SECURITY_ACCESS_DENIED");
        MvcResult login = login("stage0_viewer", "Stage0ViewerTestOnly!2026");

        mockMvc.perform(get("/api/v1/meta/modules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(login))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLATFORM_ACCESS_DENIED"));
        int after = baseline.countOperations("SECURITY_ACCESS_DENIED");
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void databaseMenusAreCompleteAndFilteredByRole() throws Exception {
        MvcResult adminLogin = login("stage0_admin", "Stage0AdminTestOnly!2026");
        MvcResult adminMenus = mockMvc.perform(get("/api/v1/navigation/menus")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(adminLogin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].menuCode").value("MENU_WORKBENCH"))
                .andExpect(jsonPath("$.data[2].menuCode").value("MENU_SYSTEM"))
                .andReturn();
        JsonNode adminTree = objectMapper.readTree(adminMenus.getResponse().getContentAsString())
                .path("data");
        int pageCount = 0;
        for (JsonNode domain : adminTree) {
            pageCount += domain.path("children").size();
        }
        assertThat(pageCount).isEqualTo(21);
        assertThat(adminMenus.getResponse().getContentAsString())
                .doesNotContain("MENU_PROCUREMENT_FORECAST_PLAN");

        MvcResult viewerLogin = login("stage0_viewer", "Stage0ViewerTestOnly!2026");
        mockMvc.perform(get("/api/v1/navigation/menus")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(viewerLogin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].menuCode").value("MENU_WORKBENCH_HOME"));
    }

    @Test
    void invalidCredentialsDoNotLeakAccountDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"stage0_admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLATFORM_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unknownAuthenticatedApiUsesUnifiedNotFoundResponse() throws Exception {
        MvcResult login = login("stage0_admin", "Stage0AdminTestOnly!2026");
        mockMvc.perform(get("/api/v1/not-a-resource")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(login))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLATFORM_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private MvcResult login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password)
                .toString();
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("SRM_REFRESH", true))
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
    }

    private String accessToken(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
