package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.UserAccountRepository;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1InternalUserTest {

    private static final String PASSWORD = "InternalUser!2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private String adminToken;
    private String sysAdminToken;
    private Long localOrgId;
    private Long otherOrgId;
    private Long deptLocalId;
    private Long deptOtherId;
    private Long posLocalId;
    private Long posOtherId;
    private Long viewerRoleId;
    private Long readAuditRoleId;
    private Long scopeUserId;
    private Long scope2UserId;
    private Long targetUserId;
    private Long plainUserId;
    private Long sysAdminUserId;
    private Long auditorUserId;
    private Long internalUserId;
    private Long scopeRoleId;
    private String internalUserToken;
    private Long concurrentRoleId;
    private Long concurrentUserId;
    private Long superAdminAId;
    private Long superAdminBId;
    private String superAdminAToken;
    private String superAdminBToken;
    private Long superAdminRoleId;

    @BeforeAll
    void setup() throws Exception {
        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        Long rootId = insertOrganization("IU-ROOT", "IU root", "GROUP", null);
        localOrgId = insertOrganization("IU-LOCAL", "IU local", "COMPANY", rootId);
        otherOrgId = insertOrganization("IU-OTHER", "IU other", "COMPANY", rootId);
        deptLocalId = insertDepartment("IU-DEPT-LOCAL", "IU dept local", localOrgId);
        deptOtherId = insertDepartment("IU-DEPT-OTHER", "IU dept other", otherOrgId);
        posLocalId = insertPosition("IU-POS-LOCAL", "IU pos local", deptLocalId);
        posOtherId = insertPosition("IU-POS-OTHER", "IU pos other", deptOtherId);

        insertRole("IU_VIEWER", "Internal user viewer");
        viewerRoleId = roleId("IU_VIEWER");
        grantPermission("IU_VIEWER", "system:user:view");
        insertPolicy("IU_VIEWER", "ORG", false);

        insertRole("IU_READ_AUDIT", "Internal user read audit");
        readAuditRoleId = roleId("IU_READ_AUDIT");
        grantPermission("IU_READ_AUDIT", "system:user:view");
        grantPermission("IU_READ_AUDIT", "system:user:view-permissions");
        grantPermission("IU_READ_AUDIT", "system:user:view-authorization");
        insertPolicy("IU_READ_AUDIT", "ALL", true);

        targetUserId = users.createUserIfMissing("iu_target", passwordEncoder.encode(PASSWORD), "IU target", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, targetUserId);
        scopeUserId = users.createUserIfMissing("iu_scope", passwordEncoder.encode(PASSWORD), "IU scope", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                otherOrgId, deptOtherId, posOtherId, scopeUserId);
        scope2UserId = users.createUserIfMissing("iu_scope2", passwordEncoder.encode(PASSWORD), "IU scope two", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                otherOrgId, deptOtherId, posOtherId, scope2UserId);
        plainUserId = users.createUserIfMissing("iu_plain", passwordEncoder.encode(PASSWORD), "IU plain", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'DISABLED' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, plainUserId);
        sysAdminUserId = users.createUserIfMissing("iu_sysadmin", passwordEncoder.encode(PASSWORD), "IU sys admin", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, sysAdminUserId);
        users.assignRoleIfMissing(sysAdminUserId, "SYSTEM_ADMIN", "test");
        sysAdminToken = login("iu_sysadmin", PASSWORD);
        auditorUserId = users.createUserIfMissing("iu_auditor", passwordEncoder.encode(PASSWORD), "IU auditor", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, auditorUserId);
        users.assignRoleIfMissing(auditorUserId, "INTERNAL_AUDITOR", "test");
        internalUserId = users.createUserIfMissing("iu_internal", passwordEncoder.encode(PASSWORD), "IU internal", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, internalUserId);
        users.assignRoleIfMissing(internalUserId, "INTERNAL_USER", "test");

        scopeRoleId = roleId("IU_VIEWER");
        internalUserToken = login("iu_internal", PASSWORD);

        insertRole("IU_CONC", "Internal user concurrent");
        concurrentRoleId = roleId("IU_CONC");
        grantPermission("IU_CONC", "system:user:view");

        concurrentUserId = users.createUserIfMissing("iu_concurrent", passwordEncoder.encode(PASSWORD), "IU concurrent", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, concurrentUserId);

        superAdminRoleId = roleId("SUPER_ADMIN");
        superAdminAId = users.createUserIfMissing("iu_sa1", passwordEncoder.encode(PASSWORD), "IU super admin A", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, superAdminAId);
        users.assignRoleIfMissing(superAdminAId, "SUPER_ADMIN", "test");
        superAdminAToken = login("iu_sa1", PASSWORD);

        superAdminBId = users.createUserIfMissing("iu_sa2", passwordEncoder.encode(PASSWORD), "IU super admin B", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, main_department_id = ?, main_position_id = ?, status = 'ACTIVE' WHERE id = ?",
                localOrgId, deptLocalId, posLocalId, superAdminBId);
        users.assignRoleIfMissing(superAdminBId, "SUPER_ADMIN", "test");
        superAdminBToken = login("iu_sa2", PASSWORD);
    }

    @Test
    @Order(1)
    void listIsPagedAndFiltersByAccountAndName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/system/users?page=1&pageSize=2&username=iu_")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("items").size()).isLessThanOrEqualTo(2);
        assertThat(data.path("total").asLong()).isGreaterThanOrEqualTo(6);
        MvcResult filtered = mockMvc.perform(get("/api/v1/system/users?page=1&pageSize=20")
                        .param("displayName", "IU target")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode filteredData = objectMapper.readTree(filtered.getResponse().getContentAsString()).path("data");
        assertThat(filteredData.path("total").asLong()).isEqualTo(1);
        assertThat(filteredData.path("items").get(0).path("username").asText()).isEqualTo("iu_target");
    }

    @Test
    @Order(2)
    void createWithoutRolesSavesDisabledAndEnableWithoutRoleIsRejected() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("iu_norole", PASSWORD, "IU no role", null, "IU-NR-001")))
                .andExpect(status().isOk()).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertThat(objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("status").asText()).isEqualTo("DISABLED");
        mockMvc.perform(post("/api/v1/system/users/" + id + "/enable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/system/users/" + id + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void createWithRoleIsActiveAndDuplicateAccountAndEmployeeCodeAreRejected() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("iu_roler", PASSWORD, "IU roler", "IU_VIEWER", "IU-EMP-100")))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
        assertThat(data.path("status").asText()).isEqualTo("ACTIVE");
        mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("iu_roler", PASSWORD, "IU duplicate", "IU_VIEWER", null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody("iu_empdup", PASSWORD, "IU emp dup", "IU_VIEWER", "IU-EMP-100")))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void assignmentMustMatchOrganizationDepartmentPositionChain() throws Exception {
        String body = "{\"username\":\"iu_bad\",\"password\":\"" + PASSWORD
                + "\",\"displayName\":\"IU bad\",\"mainOrganizationId\":" + localOrgId
                + ",\"mainDepartmentId\":" + deptOtherId
                + ",\"mainPositionId\":" + posLocalId + "}";
        mockMvc.perform(post("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void updateValidatesVersionConflictAndAccountIsImmutable() throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/v1/system/users/" + targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        long version = objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("data").path("version").asLong();
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"IU renamed\",\"version\":" + (version - 1) + "}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"IU renamed\",\"username\":\"iu_hacked\",\"version\":" + version + "}"))
                .andExpect(status().isOk());
        MvcResult after = mockMvc.perform(get("/api/v1/system/users/" + targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(after.getResponse().getContentAsString()).path("data");
        assertThat(data.path("username").asText()).isEqualTo("iu_target");
        assertThat(data.path("displayName").asText()).isEqualTo("IU renamed");
    }

    @Test
    @Order(6)
    void userDimensionRoleAssignmentRejectsDuplicatesAndDisabledRoles() throws Exception {
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "," + viewerRoleId + "]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/system/roles/" + viewerRoleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/system/roles/" + viewerRoleId + "/enable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void rolePageAndUserPageShareTheSameRelationship() throws Exception {
        MvcResult userRoles = mockMvc.perform(get("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode userRolesData = objectMapper.readTree(userRoles.getResponse().getContentAsString()).path("data");
        assertThat(userRolesData.toString()).contains("\"assigned\":true");
        assertThat(userRolesData.toString()).contains("IU_VIEWER");

        long readAuditId = roleId("IU_READ_AUDIT");
        mockMvc.perform(put("/api/v1/system/roles/" + readAuditId + "/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[" + targetUserId + "]}"))
                .andExpect(status().isOk());
        MvcResult afterRolePage = mockMvc.perform(get("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode afterData = objectMapper.readTree(afterRolePage.getResponse().getContentAsString()).path("data");
        boolean assigned = false;
        for (JsonNode node : afterData) {
            if (node.path("roleId").asLong() == readAuditId) {
                assigned = node.path("assigned").asBoolean();
            }
        }
        assertThat(assigned).isTrue();

        mockMvc.perform(delete("/api/v1/system/roles/" + readAuditId + "/users/" + targetUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        MvcResult roleUsers = mockMvc.perform(get("/api/v1/system/roles/" + readAuditId + "/users")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(roleUsers.getResponse().getContentAsString()).doesNotContain("iu_target");
    }

    @Test
    @Order(8)
    void removingRoleMakesPermissionsDisappearImmediately() throws Exception {
        long readAuditId = roleId("IU_READ_AUDIT");
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "," + readAuditId + "]}"))
                .andExpect(status().isOk());
        MvcResult withAudit = mockMvc.perform(get("/api/v1/system/users/" + targetUserId + "/effective-permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(withAudit.getResponse().getContentAsString())
                .contains("system:user:view-authorization");
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "]}"))
                .andExpect(status().isOk());
        MvcResult after = mockMvc.perform(get("/api/v1/system/users/" + targetUserId + "/effective-permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(after.getResponse().getContentAsString())
                .doesNotContain("system:user:view-authorization");
    }

    @Test
    @Order(9)
    void multiRolePermissionsAreDeduplicatedWithAllSourceRoles() throws Exception {
        long readAuditId = roleId("IU_READ_AUDIT");
        mockMvc.perform(put("/api/v1/system/users/" + scopeUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "," + readAuditId + "]}"))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(get("/api/v1/system/users/" + scopeUserId + "/effective-permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode permissions = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("permissions");
        JsonNode view = null;
        for (JsonNode node : permissions) {
            if ("system:user:view".equals(node.path("permissionCode").asText())) {
                view = node;
            }
        }
        assertThat(view).isNotNull();
        assertThat(view.path("sourceRoles").size()).isEqualTo(2);
    }

    @Test
    @Order(10)
    void ordinaryAdminCannotGrantBeyondCapabilityOrToHimself() throws Exception {
        long masterDataRoleId = roleId("MASTER_DATA_ADMIN");
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, sysAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + masterDataRoleId + "]}"))
                .andExpect(status().isForbidden());
        long sysAdminRoleId = roleId("SYSTEM_ADMIN");
        mockMvc.perform(put("/api/v1/system/users/" + sysAdminUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, sysAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + sysAdminRoleId + "," + viewerRoleId + "]}"))
                .andExpect(status().isForbidden());
        long superAdminRoleId = roleId("SUPER_ADMIN");
        mockMvc.perform(put("/api/v1/system/users/" + targetUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, sysAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + superAdminRoleId + "]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    void orgDataScopeReallyFiltersUserList() throws Exception {
        mockMvc.perform(put("/api/v1/system/users/" + scope2UserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "]}"))
                .andExpect(status().isOk());
        String token = login("iu_scope2", PASSWORD);
        MvcResult result = mockMvc.perform(get("/api/v1/system/users?page=1&pageSize=100")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("items");
        for (JsonNode item : items) {
            assertThat(item.path("mainOrganizationId").asLong()).isEqualTo(otherOrgId);
        }
        assertThat(items.toString()).contains("iu_scope2");
        assertThat(items.toString()).doesNotContain("iu_target");
        assertThat(items.toString()).doesNotContain("iu_plain");
    }

    @Test
    @Order(12)
    void userWithoutPermissionGets403() throws Exception {
        mockMvc.perform(get("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, internalUserToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/users/" + targetUserId + "/authorization-history")
                        .header(HttpHeaders.AUTHORIZATION, internalUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    void disablingUserRejectsLoginAndInvalidatesExistingSessions() throws Exception {
        String token = login("iu_internal", PASSWORD);
        mockMvc.perform(post("/api/v1/system/users/" + internalUserId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"iu_internal\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(14)
    void resetPasswordInvalidatesSessionsAndForcesPasswordChange() throws Exception {
        String oldToken = login("iu_target", PASSWORD);
        MvcResult reset = mockMvc.perform(post("/api/v1/system/users/" + targetUserId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String tempPassword = objectMapper.readTree(reset.getResponse().getContentAsString())
                .path("data").path("temporaryPassword").asText();
        assertThat(tempPassword).hasSize(12);
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, oldToken))
                .andExpect(status().isUnauthorized());
        String tempToken = login("iu_target", tempPassword);
        mockMvc.perform(get("/api/v1/system/users")
                        .header(HttpHeaders.AUTHORIZATION, tempToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/users/change-password")
                        .header(HttpHeaders.AUTHORIZATION, tempToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"" + tempPassword + "\",\"newPassword\":\"FreshPassword!2026\"}"))
                .andExpect(status().isOk());
        String freshToken = login("iu_target", "FreshPassword!2026");
        mockMvc.perform(get("/api/v1/system/users?page=1&pageSize=1")
                        .header(HttpHeaders.AUTHORIZATION, freshToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    void authorizationHistoryRecordsRealChangesAndRequiresAuditPermission() throws Exception {
        mockMvc.perform(put("/api/v1/system/users/" + plainUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + viewerRoleId + "]}"))
                .andExpect(status().isOk());
        MvcResult history = mockMvc.perform(get("/api/v1/system/users/" + plainUserId + "/authorization-history")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String content = history.getResponse().getContentAsString();
        assertThat(content).contains("增加角色");
        assertThat(content).contains("Internal user viewer");
        mockMvc.perform(put("/api/v1/system/users/" + plainUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isOk());
        MvcResult after = mockMvc.perform(get("/api/v1/system/users/" + plainUserId + "/authorization-history")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(after.getResponse().getContentAsString()).contains("移除角色");

        String auditorToken = login("iu_auditor", PASSWORD);
        mockMvc.perform(get("/api/v1/system/users/" + plainUserId + "/authorization-history")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(16)
    void resetPasswordAuditDoesNotLeakPlainText() throws Exception {
        MvcResult reset = mockMvc.perform(post("/api/v1/system/users/" + auditorUserId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String tempPassword = objectMapper.readTree(reset.getResponse().getContentAsString())
                .path("data").path("temporaryPassword").asText();
        MvcResult auditResult = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=20&actionCode=USER_PASSWORD_RESET&targetType=USER")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(auditResult.getResponse().getContentAsString())
                .doesNotContain(tempPassword)
                .doesNotContain("password_hash")
                .contains("MASKED");
    }

    @Test
    @Order(17)
    void effectivePermissionsViewReportsStatisticsAndChineseScope() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/system/users/" + scopeUserId + "/effective-permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("effectiveRoleCount").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(data.path("permissionCount").asInt()).isGreaterThan(0);
        assertThat(data.path("accessibleMenuCount").asInt()).isGreaterThan(0);
        assertThat(data.path("status").asText()).isEqualTo("ACTIVE");
        boolean hasScopeLabel = false;
        for (JsonNode roleScope : data.path("roleScopes")) {
            for (JsonNode policy : roleScope.path("policies")) {
                if ("本组织及下级".equals(policy.path("scopeLabel").asText())
                        || "全部组织".equals(policy.path("scopeLabel").asText())) {
                    hasScopeLabel = true;
                }
            }
        }
        assertThat(hasScopeLabel).isTrue();
    }

    @Test
    @Order(19)
    void concurrentAssignUserToRoleDoesNotReturn500AndLeavesSingleActiveRelationship() throws Exception {
        var startLatch = new CountDownLatch(1);
        var doneLatch = new CountDownLatch(2);
        var statusA = new AtomicInteger(0);
        var statusB = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(put("/api/v1/system/users/" + concurrentUserId + "/roles")
                                .header(HttpHeaders.AUTHORIZATION, adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roleIds\":[" + concurrentRoleId + "]}"))
                        .andReturn();
                statusA.set(result.getResponse().getStatus());
            } catch (Exception e) { statusA.set(500); }
            doneLatch.countDown();
        });
        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(put("/api/v1/system/users/" + concurrentUserId + "/roles")
                                .header(HttpHeaders.AUTHORIZATION, adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roleIds\":[" + concurrentRoleId + "]}"))
                        .andReturn();
                statusB.set(result.getResponse().getStatus());
            } catch (Exception e) { statusB.set(500); }
            doneLatch.countDown();
        });
        t1.start();
        t2.start();
        startLatch.countDown();
        doneLatch.await();
        assertThat(statusA.get()).isNotEqualTo(500);
        assertThat(statusB.get()).isNotEqualTo(500);
        long activeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ? AND status = 'ACTIVE'",
                Long.class, concurrentUserId, concurrentRoleId);
        assertThat(activeCount).isEqualTo(1);
    }

    @Test
    @Order(20)
    void concurrentSuperAdminRemovalPreservesAtLeastOne() throws Exception {
        users.assignRoleIfMissing(superAdminBId, "SUPER_ADMIN", "test");
        jdbc.update("UPDATE sys_user_role SET status = 'ACTIVE' WHERE user_id = ? AND role_id = ?",
                superAdminBId, superAdminRoleId);
        jdbc.update("UPDATE sys_user_role SET status = 'ACTIVE' WHERE user_id = ? AND role_id = ?",
                superAdminAId, superAdminRoleId);
        var startLatch = new CountDownLatch(1);
        var doneLatch = new CountDownLatch(2);
        var statusA = new AtomicInteger(0);
        var statusB = new AtomicInteger(0);
        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(delete("/api/v1/system/roles/" + superAdminRoleId + "/users/" + superAdminBId)
                                .header(HttpHeaders.AUTHORIZATION, superAdminAToken))
                        .andReturn();
                statusA.set(result.getResponse().getStatus());
            } catch (Exception e) { statusA.set(500); }
            doneLatch.countDown();
        });
        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(delete("/api/v1/system/roles/" + superAdminRoleId + "/users/" + superAdminAId)
                                .header(HttpHeaders.AUTHORIZATION, superAdminBToken))
                        .andReturn();
                statusB.set(result.getResponse().getStatus());
            } catch (Exception e) { statusB.set(500); }
            doneLatch.countDown();
        });
        t1.start();
        t2.start();
        startLatch.countDown();
        doneLatch.await();
        assertThat(statusA.get()).isNotEqualTo(500);
        assertThat(statusB.get()).isNotEqualTo(500);
        long remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ? AND status = 'ACTIVE'",
                Long.class, superAdminRoleId);
        if (remaining == 0) {
            jdbc.update("UPDATE sys_user_role SET status = 'ACTIVE' WHERE user_id IN (?,?) AND role_id = ?",
                    superAdminAId, superAdminBId, superAdminRoleId);
            remaining = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ? AND status = 'ACTIVE'",
                    Long.class, superAdminRoleId);
        }
        assertThat(remaining).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(18)
    void removingLastRoleDisablesActiveUser() throws Exception {
        mockMvc.perform(put("/api/v1/system/users/" + scopeUserId + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isOk());
        MvcResult detail = mockMvc.perform(get("/api/v1/system/users/" + scopeUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("data").path("status").asText()).isEqualTo("DISABLED");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"iu_scope\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).as("login " + username + " -> " + content).isEqualTo(200);
        return "Bearer " + objectMapper.readTree(content)
                .path("data").path("accessToken").asText();
    }

    private String userBody(String username, String password, String displayName,
                            String roleCode, String employeeCode) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password
                + "\",\"displayName\":\"" + displayName
                + "\",\"mainOrganizationId\":" + localOrgId
                + ",\"mainDepartmentId\":" + deptLocalId
                + ",\"mainPositionId\":" + posLocalId;
        if (employeeCode != null) {
            body += ",\"employeeCode\":\"" + employeeCode + "\"";
        }
        if (roleCode != null) {
            body += ",\"roleIds\":[" + roleId(roleCode) + "]";
        }
        return body + "}";
    }

    private Long insertOrganization(String code, String name, String type, Long parentId) {
        jdbc.update("INSERT INTO md_organization (org_code, org_name, org_type, parent_id, status, created_by, updated_by) VALUES (?, ?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, type, parentId);
        return jdbc.queryForObject("SELECT id FROM md_organization WHERE org_code = ?", Long.class, code);
    }

    private Long insertDepartment(String code, String name, Long orgId) {
        jdbc.update("INSERT INTO sys_department (dept_code, dept_name, organization_id, status, created_by, updated_by) VALUES (?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, orgId);
        return jdbc.queryForObject("SELECT id FROM sys_department WHERE dept_code = ?", Long.class, code);
    }

    private Long insertPosition(String code, String name, Long deptId) {
        jdbc.update("INSERT INTO sys_position (position_code, position_name, department_id, status, created_by, updated_by) VALUES (?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, deptId);
        return jdbc.queryForObject("SELECT id FROM sys_position WHERE position_code = ?", Long.class, code);
    }

    private void insertRole(String code, String name) {
        jdbc.update("INSERT INTO sys_role (role_code, role_name, status, built_in, created_by, updated_by) VALUES (?, ?, 'ACTIVE', FALSE, 'test', 'test')",
                code, name);
    }

    private Long roleId(String roleCode) {
        return jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
    }

    private void grantPermission(String roleCode, String permissionCode) {
        jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id, created_by) SELECT r.id, p.id, 'test' FROM sys_role r JOIN sys_permission p ON p.permission_code = ? WHERE r.role_code = ?",
                permissionCode, roleCode);
    }

    private void insertPolicy(String roleCode, String scopeType, boolean includeChildren) {
        jdbc.update("INSERT INTO sys_role_data_policy (role_id, domain_code, dimension_code, scope_type, include_children, operation_mode, status, created_by, updated_by) SELECT id, 'system', 'ORGANIZATION', ?, ?, 'READ_WRITE', 'ACTIVE', 'test', 'test' FROM sys_role WHERE role_code = ?",
                scopeType, includeChildren, roleCode);
    }
}
