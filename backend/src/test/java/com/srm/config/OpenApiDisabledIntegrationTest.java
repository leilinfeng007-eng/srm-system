package com.srm.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class OpenApiDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void disabledDocumentationEndpointsAreUnavailableWhileBusinessApisRemainAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLATFORM_RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/v3/api-docs/internal"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLATFORM_RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/v3/api-docs/supplier"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLATFORM_RESOURCE_NOT_FOUND"));
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLATFORM_RESOURCE_NOT_FOUND"));

        String token = login();
        mockMvc.perform(get("/api/v1/workbench/baseline")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"stage0_admin","password":"Stage0AdminTestOnly!2026"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("accessToken").asText();
    }
}
