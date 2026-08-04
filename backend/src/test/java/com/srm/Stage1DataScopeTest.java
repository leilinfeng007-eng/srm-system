package com.srm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.masterdata.infrastructure.persistence.entity.MdPurchasingOrganizationEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdPurchasingOrganizationMapper;
import com.srm.security.auth.UserAccountRepository;
import java.util.Map;
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
class Stage1DataScopeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MdPurchasingOrganizationMapper poMapper;

    private String adminToken;
    private String mixedScopeToken;
    private Long localOrgId;
    private Long siblingOrgId;

    @BeforeAll
    void setup() throws Exception {
        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        Long rootId = insertOrganization("SCOPE-ROOT", "Scope root", "GROUP", null);
        localOrgId = insertOrganization("SCOPE-LOCAL", "Scope local", "COMPANY", rootId);
        siblingOrgId = insertOrganization("SCOPE-SIBLING", "Scope sibling", "COMPANY", rootId);

        insertPurchasingOrganization("SCOPE-PO-LOCAL", localOrgId);
        insertPurchasingOrganization("SCOPE-PO-SIBLING", siblingOrgId);

        insertRole("SCOPE_GLOBAL_READ", "Scope global read");
        insertRole("SCOPE_LOCAL_WRITE", "Scope local write");
        grantPermission("SCOPE_GLOBAL_READ", "masterdata:purchasing-organization:view");
        grantPermission("SCOPE_GLOBAL_READ", "masterdata:purchasing-organization:create");
        grantPermission("SCOPE_LOCAL_WRITE", "masterdata:purchasing-organization:view");
        grantPermission("SCOPE_LOCAL_WRITE", "masterdata:purchasing-organization:create");
        insertPolicy("SCOPE_GLOBAL_READ", "ALL", "READONLY");
        insertPolicy("SCOPE_LOCAL_WRITE", "ORG", "READ_WRITE");

        long userId = users.createUserIfMissing("scope_mixed", passwordEncoder.encode("ScopeMixed!2026"),
                "Scope mixed", "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ? WHERE id = ?", localOrgId, userId);
        users.assignRoleIfMissing(userId, "SCOPE_GLOBAL_READ", "test");
        users.assignRoleIfMissing(userId, "SCOPE_LOCAL_WRITE", "test");
        mixedScopeToken = login("scope_mixed", "ScopeMixed!2026");
    }

    @Test
    @Order(1)
    void superAdminHasExplicitAllReadWritePath() throws Exception {
        mockMvc.perform(post("/api/v1/master-data/purchasing-organizations")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody("SCOPE-PO-ADMIN", siblingOrgId)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void readMergesAllScopeAcrossEligibleRolePaths() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/master-data/purchasing-organizations?page=1&size=100")
                        .header(HttpHeaders.AUTHORIZATION, mixedScopeToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("items").toString()).contains("SCOPE-PO-LOCAL", "SCOPE-PO-SIBLING");
    }

    @Test
    @Order(3)
    void writeDoesNotAmplifyReadOnlyAllWithOrgReadWrite() throws Exception {
        mockMvc.perform(post("/api/v1/master-data/purchasing-organizations")
                        .header(HttpHeaders.AUTHORIZATION, mixedScopeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody("SCOPE-PO-BLOCKED", siblingOrgId)))
                .andExpect(status().isNotFound());
        assertThat(findByCode("SCOPE-PO-BLOCKED")).isNull();
    }

    @Test
    @Order(4)
    void writeSucceedsInsideSameRoleOrgPath() throws Exception {
        mockMvc.perform(post("/api/v1/master-data/purchasing-organizations")
                        .header(HttpHeaders.AUTHORIZATION, mixedScopeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(poBody("SCOPE-PO-ALLOWED", localOrgId)))
                .andExpect(status().isOk());
        assertThat(findByCode("SCOPE-PO-ALLOWED")).isNotNull();
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode().put("username", username)
                .put("password", password).toString();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long insertOrganization(String code, String name, String type, Long parentId) {
        jdbc.update("INSERT INTO md_organization (org_code, org_name, org_type, parent_id, status, created_by, updated_by) VALUES (?, ?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, type, parentId);
        return jdbc.queryForObject("SELECT id FROM md_organization WHERE org_code = ?", Long.class, code);
    }

    private void insertPurchasingOrganization(String code, Long companyOrgId) {
        jdbc.update("INSERT INTO md_purchasing_organization (po_code, po_name, company_org_id, default_currency, status, source_type, source_system, created_by, updated_by) VALUES (?, ?, ?, 'CNY', 'ACTIVE', 'MANUAL', 'SRM', 'test', 'test')",
                code, code, companyOrgId);
    }

    private void insertRole(String code, String name) {
        jdbc.update("INSERT INTO sys_role (role_code, role_name, status, built_in, created_by, updated_by) VALUES (?, ?, 'ACTIVE', FALSE, 'test', 'test')",
                code, name);
    }

    private void grantPermission(String roleCode, String permissionCode) {
        jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id, created_by) SELECT r.id, p.id, 'test' FROM sys_role r JOIN sys_permission p ON p.permission_code = ? WHERE r.role_code = ?",
                permissionCode, roleCode);
    }

    private void insertPolicy(String roleCode, String scopeType, String operationMode) {
        jdbc.update("INSERT INTO sys_role_data_policy (role_id, domain_code, dimension_code, scope_type, include_children, operation_mode, status, created_by, updated_by) SELECT id, 'masterdata', 'PURCHASING_ORGANIZATION', ?, TRUE, ?, 'ACTIVE', 'test', 'test' FROM sys_role WHERE role_code = ?",
                scopeType, operationMode, roleCode);
    }

    private String poBody(String code, Long companyOrgId) throws Exception {
        return objectMapper.writeValueAsString(Map.of("poCode", code, "poName", code,
                "companyOrgId", companyOrgId, "defaultCurrency", "CNY"));
    }

    private MdPurchasingOrganizationEntity findByCode(String code) {
        return poMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdPurchasingOrganizationEntity>()
                .eq(MdPurchasingOrganizationEntity::getPoCode, code));
    }
}
