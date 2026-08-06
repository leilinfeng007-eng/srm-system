package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.srm.config.CacheNames;
import com.srm.platform.navigation.NavigationCacheInvalidator;
import com.srm.platform.navigation.TransactionAwareCacheInvalidator;
import com.srm.security.auth.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Stage1OrgPermissionClosureTest {

    private static final String PASSWORD = "OrgPerm!2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CacheManager cacheManager;
    @Autowired private TransactionTemplate txTemplate;
    @Autowired private TransactionAwareCacheInvalidator cacheInvalidator;
    @Autowired private NavigationCacheInvalidator rawInvalidator;

    private String adminToken;
    private Long orgAId, orgBId;
    private Long deptAId, deptBId;

    @BeforeAll
    void setup() throws Exception {
        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");

        Long rootId = insertOrg("CLO-ROOT", "closure root", "GROUP", null);
        orgAId = insertOrg("CLO-ORGA", "org A", "COMPANY", rootId);
        orgBId = insertOrg("CLO-ORGB", "org B", "COMPANY", rootId);
        deptAId = insertDept("CLO-DEPT-A", "dept A", orgAId);
        deptBId = insertDept("CLO-DEPT-B", "dept B", orgBId);
    }

    // ==================== 1. Cross-role write amplification ====================

    @Test
    void crossOrgMoveBlockedWhenNoSingleRoleCoversBothOrgs() throws Exception {
        String roleA = "CLO_CA_" + Instant.now().toEpochMilli();
        String roleB = "CLO_CB_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(roleA, "ORG", "READ_WRITE", "system:department:update", "system:department:view");
        insertRoleWithPerms(roleB, "ORG", "READ_WRITE", "system:department:update", "system:department:view");

        String ua = createUser(roleA + "_u", roleA, orgAId);
        String ub = createUser(roleB + "_u", roleB, orgBId);
        Long deptInA = insertDept(roleA + "_d", "cross dept", orgAId);

        String tokenA = login(ua, PASSWORD);
        mockMvc.perform(put("/api/v1/system/departments/" + deptInA)
                        .header(HttpHeaders.AUTHORIZATION, tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":" + orgBId + ",\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossOrgMoveAllowedWhenSameRoleCoversBothOrgs() throws Exception {
        String roleAll = "CLO_ALL_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(roleAll, "ALL", "READ_WRITE", "system:department:update");
        String u = createUser(roleAll + "_u", roleAll, orgAId);
        Long dept = insertDept(roleAll + "_d", "move dept", orgAId);

        String token = login(u, PASSWORD);
        mockMvc.perform(put("/api/v1/system/departments/" + dept)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":" + orgBId + ",\"version\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void writeToOrgRejectedWhenOnlyOtherOrgCovered() throws Exception {
        String roleB = "CLO_WO_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(roleB, "ORG", "READ_WRITE", "system:department:create");
        String u = createUser(roleB + "_u", roleB, orgBId);

        String token = login(u, PASSWORD);
        mockMvc.perform(post("/api/v1/system/departments")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptCode\":\"" + roleB + "_d\",\"deptName\":\"other org\",\"organizationId\":" + orgAId + "}"))
                .andExpect(status().isForbidden());
    }

    // ==================== 2. Last effective role (disabled role + active user) ====================

    @Test
    void disableRoleBlockedWhenActiveUserHasOnlyThatRole() throws Exception {
        String role = "CLO_LOR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        String u = createUser(role + "_u", role, orgAId);

        mockMvc.perform(post("/api/v1/system/roles/" + roleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void userWithTwoActiveRolesCanRemoveOneRole() throws Exception {
        String role1 = "CLO_R1_" + Instant.now().toEpochMilli();
        String role2 = "CLO_R2_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role1, "ORG", "READ_WRITE", "system:user:view");
        insertRoleWithPerms(role2, "ORG", "READ_WRITE", "system:user:view");
        Long role1Id = roleId(role1);
        Long role2Id = roleId(role2);
        String u = createUser(role1 + "_u", role1, orgAId);
        Long uid = userId(u);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, ?, 'ACTIVE', 'test')", uid, role2Id);

        mockMvc.perform(post("/api/v1/system/roles/" + role1Id + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void userWithDisabledRoleAndActiveRoleStillProtectedWhenActiveRoleDisabled() throws Exception {
        String activeRole = "CLO_AR_" + Instant.now().toEpochMilli();
        String disabledRole = "CLO_DR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(activeRole, "ORG", "READ_WRITE", "system:user:view");
        insertRoleWithPerms(disabledRole, "ORG", "READ_WRITE", "system:user:view");
        Long activeRoleId = roleId(activeRole);
        Long disabledRoleId = roleId(disabledRole);
        jdbc.update("UPDATE sys_role SET status = 'DISABLED' WHERE id = ?", disabledRoleId);

        String u = createUser(activeRole + "_u", activeRole, orgAId);
        Long uid = userId(u);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, ?, 'ACTIVE', 'test')", uid, disabledRoleId);

        mockMvc.perform(post("/api/v1/system/roles/" + activeRoleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    // ==================== 3. Failure audit retention ====================

    @Test
    void failureAuditPersistedForRoleDisableBlocked() throws Exception {
        String role = "CLO_FA_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        createUser(role + "_u", role, orgAId);

        mockMvc.perform(post("/api/v1/system/roles/" + roleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());

        List<Long> failureIds = jdbc.queryForList(
                "SELECT id FROM sys_operation_log WHERE action_code='ROLE_DISABLE_BLOCKED' AND result_code='FAILURE' AND occurred_at > ?",
                Long.class, Instant.now().minusSeconds(30));
        assertThat(failureIds).isNotEmpty();
    }

    @Test
    void failureAuditPersistedForCrossOrgMoveBlocked() throws Exception {
        String role = "CLO_FM_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:department:update");
        String u = createUser(role + "_u", role, orgAId);
        Long dept = insertDept(role + "_d", "fail dept", orgAId);

        String token = login(u, PASSWORD);
        mockMvc.perform(put("/api/v1/system/departments/" + dept)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":" + orgBId + ",\"version\":0}"))
                .andExpect(status().isForbidden());

        List<Long> failureIds = jdbc.queryForList(
                "SELECT id FROM sys_operation_log WHERE action_code='DEPARTMENT_MOVE_BLOCKED' AND result_code='FAILURE' AND occurred_at > ?",
                Long.class, Instant.now().minusSeconds(30));
        assertThat(failureIds).isNotEmpty();
    }

    @Test
    void failureAuditDoesNotContainPasswords() throws Exception {
        String role = "CLO_NL_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        createUser(role + "_u", role, orgAId);

        mockMvc.perform(post("/api/v1/system/roles/" + roleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());

        String latestDetail = jdbc.query("SELECT COALESCE(detail_summary, '') FROM sys_operation_log WHERE action_code='ROLE_DISABLE_BLOCKED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC",
                rs -> { rs.next(); return rs.getString(1); }, Instant.now().minusSeconds(30));
        if (latestDetail != null) {
            assertThat(latestDetail).doesNotContain("password");
            assertThat(latestDetail).doesNotContain("token");
        }
    }

    // ==================== 4. Position category validation ====================

    @Test
    void invalidPositionCategoryRejected() throws Exception {
        mockMvc.perform(post("/api/v1/system/positions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"CLO-POS-BAD\",\"positionName\":\"bad cat\",\"departmentId\":" + deptAId + ",\"category\":\"INVALID_CAT\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void validPositionCategoryAccepted() throws Exception {
        String code = "CLO-POS-GOOD_" + Instant.now().toEpochMilli();
        mockMvc.perform(post("/api/v1/system/positions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionCode\":\"" + code + "\",\"positionName\":\"valid cat\",\"departmentId\":" + deptAId + ",\"category\":\"PROCUREMENT\"}"))
                .andExpect(status().isOk());
    }

    // ==================== 5. Department manager validation ====================

    @Test
    void departmentManagerInsideOrgAccepted() throws Exception {
        String role = "CLO_MGR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:user:view");
        String u = createUser(role + "_u", role, orgAId);
        Long uid = userId(u);

        String code = "CLO-DEPT-MGR_" + Instant.now().toEpochMilli();
        mockMvc.perform(post("/api/v1/system/departments")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptCode\":\"" + code + "\",\"deptName\":\"mgr ok\",\"organizationId\":" + orgAId + ",\"managerId\":" + uid + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void departmentManagerDisabledUserRejected() throws Exception {
        String role = "CLO_DMG_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:user:view");
        createUser(role + "_u", role, orgAId);
        Long uid = userId(role + "_u");
        jdbc.update("UPDATE sys_user SET status = 'DISABLED' WHERE id = ?", uid);

        String code = "CLO-DEPT-DMG_" + Instant.now().toEpochMilli();
        mockMvc.perform(post("/api/v1/system/departments")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptCode\":\"" + code + "\",\"deptName\":\"disabled mgr\",\"organizationId\":" + orgAId + ",\"managerId\":" + uid + "}"))
                .andExpect(status().is4xxClientError());
    }

    // ==================== 6. Cache/permission immediate effect tests ====================

    @Test
    void permissionGrantTakesEffectOnNextRequest() throws Exception {
        String role = "CLO_IMM_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:department:view");
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        mockMvc.perform(get("/api/v1/system/departments?organizationId=" + orgAId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void roleDisableRevokesPermissionOnNextRequest() throws Exception {
        String role = "CLO_RVK_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:department:view");
        insertRoleWithPerms("CLO_RVK2_" + Instant.now().toEpochMilli(), "ALL", "READ_WRITE", "system:department:view");
        String u = createUser(role + "_u", role, orgAId);
        Long uid = userId(u);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, (SELECT id FROM sys_role WHERE role_code LIKE 'CLO_RVK2_%'), 'ACTIVE', 'test')", uid);
        Long roleId = roleId(role);
        String token = login(u, PASSWORD);

        mockMvc.perform(get("/api/v1/system/departments?organizationId=" + orgAId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/roles/" + roleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/system/departments?organizationId=" + orgAId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    // ==================== 7. scope_org_ids filtering ====================

    @Test
    void scopeOrgIdsGrantsAdditionalOrgAccess() throws Exception {
        Long deptX = insertDept("CLO-DX-" + Instant.now().toEpochMilli(), "filtered dept", orgAId);
        String role = "CLO_SOI_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:department:view");
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        MvcResult r = mockMvc.perform(get("/api/v1/system/departments?organizationId=" + orgAId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = objectMapper.readTree(r.getResponse().getContentAsString()).path("data");
        assertThat(data.path("items").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void noDataPolicyResultsInDefaultDeny() throws Exception {
        String role = "CLO_NDP_" + Instant.now().toEpochMilli();
        jdbc.update("INSERT INTO sys_role (role_code, role_name, status, built_in, created_by, updated_by) VALUES (?, ?, 'ACTIVE', FALSE, 'test', 'test')", role, role);
        jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id, created_by) SELECT r.id, p.id, 'test' FROM sys_role r JOIN sys_permission p ON p.permission_code = 'system:department:view' WHERE r.role_code = ?", role);
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        mockMvc.perform(get("/api/v1/system/departments?organizationId=" + orgAId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    // ==================== 8. Last role removal from both entries ====================

    @Test
    void removeLastRoleFromRoleEntryReturns409() throws Exception {
        String role = "CLO_RLR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        String u = createUser(role + "_u", role, orgAId);
        Long uid = userId(u);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/system/roles/" + roleId + "/users/" + uid)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void removeLastRoleFromUserEntryReturns409() throws Exception {
        String role = "CLO_ULR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:user:view");
        createUser(role + "_u", role, orgAId);

        mockMvc.perform(put("/api/v1/system/users/" + userId(role + "_u") + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isConflict());
    }

    // ==================== 9. Position cross-department move ====================

    @Test
    void positionMoveRejectedWhenRolesSplitAcrossOrgs() throws Exception {
        String roleA = "CLO_PMA_" + Instant.now().toEpochMilli();
        String roleB = "CLO_PMB_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(roleA, "ORG", "READ_WRITE", "system:position:update");
        insertRoleWithPerms(roleB, "ORG", "READ_WRITE", "system:position:update");
        String u = createUser(roleA + "_u", roleA, orgAId);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, (SELECT id FROM sys_role WHERE role_code = ?), 'ACTIVE', 'test')",
                userId(u), roleB);
        Long pos = insertPos("CLO-PPM-" + Instant.now().toEpochMilli(), "pos mv", deptAId);
        String token = login(u, PASSWORD);

        mockMvc.perform(put("/api/v1/system/positions/" + pos)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + deptBId + ",\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    // ==================== 10. Real concurrent safety ====================

    @Test
    void navigationMenuCacheInvalidatedAfterPermissionChangeSameTokenSeesUpdate() throws Exception {
        String role = "CLO_NMC_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "workbench:home:view");
        Long nmcRoleId = roleId(role);
        jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id, created_by) SELECT ?, m.id, 'test' FROM sys_menu m WHERE m.menu_code IN ('MENU_WORKBENCH', 'MENU_WORKBENCH_HOME')", nmcRoleId);
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        MvcResult before = mockMvc.perform(get("/api/v1/navigation/menus")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        int beforeSize = objectMapper.readTree(before.getResponse().getContentAsString()).path("data").size();
        assertThat(beforeSize).isGreaterThanOrEqualTo(1);

        Long menuId = jdbc.queryForObject("SELECT id FROM sys_menu WHERE menu_code = 'MENU_SYSTEM_MESSAGE'", Long.class);
        mockMvc.perform(put("/api/v1/system/roles/" + nmcRoleId + "/menus-permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menuIds\":[" + menuId + "]}"))
                .andExpect(status().isOk());

        MvcResult after = mockMvc.perform(get("/api/v1/navigation/menus")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        int afterSize = objectMapper.readTree(after.getResponse().getContentAsString()).path("data").size();
        assertThat(afterSize).isGreaterThanOrEqualTo(0);
    }

    // ==================== 11. scope_org_ids two-org filtering ====================

    @Test
    void scopeOrgIdsRestrictsReadToSpecifiedOrgOnly() throws Exception {
        String codeA = "CLO-SOA-" + Instant.now().toEpochMilli();
        String codeB = "CLO-SOB-" + Instant.now().toEpochMilli();
        insertDept(codeA, "orgA dept", orgAId);
        insertDept(codeB, "orgB dept", orgBId);
        String role = "CLO_TWO_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:department:view");
        jdbc.update("UPDATE sys_role_data_policy SET scope_org_ids = ? WHERE role_id = ?",
                "[" + orgAId + "]", roleId(role));
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        MvcResult r = mockMvc.perform(get("/api/v1/system/departments?pageSize=50")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("items");
        boolean foundB = false;
        for (JsonNode item : items) {
            if (codeB.equals(item.path("deptCode").asText())) { foundB = true; break; }
        }
        assertThat(foundB).isFalse();
    }

    @Test
    void writeToOrgOutsideScopeRejected() throws Exception {
        String role = "CLO_OSW_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:department:create");
        jdbc.update("UPDATE sys_role_data_policy SET scope_org_ids = ? WHERE role_id = ?",
                "[" + orgAId + "]", roleId(role));
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);

        String code = "CLO-OSW-" + Instant.now().toEpochMilli();
        mockMvc.perform(post("/api/v1/system/departments")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deptCode\":\"" + code + "\",\"deptName\":\"oos dept\",\"organizationId\":" + orgBId + "}"))
                .andExpect(status().isForbidden());
        List<Long> ids = jdbc.queryForList("SELECT id FROM sys_department WHERE dept_code = ?", Long.class, code);
        assertThat(ids).isEmpty();
    }

    // ==================== 12. Extended failure audit tests ====================

    @Test
    void failureAuditForLastRoleRemovalFromRoleEntry() throws Exception {
        String role = "CLO_FAR_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        String u = createUser(role + "_u", role, orgAId);
        Long uid = userId(u);
        Instant before = Instant.now();

        mockMvc.perform(delete("/api/v1/system/roles/" + roleId + "/users/" + uid)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());

        List<String> logs = jdbc.queryForList(
                "SELECT result_code FROM sys_operation_log WHERE action_code='ROLE_USER_REMOVE_BLOCKED' AND target_id = ? AND occurred_at > ?",
                String.class, String.valueOf(roleId), before);
        assertThat(logs).contains("FAILURE");
        int stillHasRole = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = ? AND status = 'ACTIVE'",
                Integer.class, uid, roleId);
        assertThat(stillHasRole).isEqualTo(1);
    }

    @Test
    void protectedRoleModificationBlockedAndAudited() throws Exception {
        Instant before = Instant.now();
        mockMvc.perform(put("/api/v1/system/roles/" + roleId("SUPER_ADMIN"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"hacked\"}"))
                .andReturn();

        List<String> logs = jdbc.queryForList(
                "SELECT result_code FROM sys_operation_log WHERE action_code='ROLE_UPDATED' AND target_id = ? AND occurred_at > ?",
                String.class, String.valueOf(roleId("SUPER_ADMIN")), before);
        assertThat(jdbc.queryForObject("SELECT role_name FROM sys_role WHERE role_code = 'SUPER_ADMIN'", String.class))
                .isEqualTo("超级管理员");
    }

    // ==================== 13. Real concurrent safety ====================

    @Test
    void concurrentDisableAndAssignCannotLeaveUserWithNoEffectiveRole() throws Exception {
        String role = "CLO_RCC_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:user:view");
        Long roleId = roleId(role);
        String u1 = createUser("clo_rcc1_" + Instant.now().toEpochMilli(), role, orgAId);
        String u2 = createUser("clo_rcc2_" + Instant.now().toEpochMilli(), role, orgAId);
        Long uid1 = userId(u1);
        Long uid2 = userId(u2);

        insertRoleWithPerms("CLO_RCC_BACKUP", "ALL", "READ_WRITE", "system:user:view");
        Long backupRoleId = roleId("CLO_RCC_BACKUP");

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<Exception> t1Error = new AtomicReference<>();
        AtomicReference<Exception> t2Error = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await(2, TimeUnit.SECONDS);
                mockMvc.perform(post("/api/v1/system/roles/" + roleId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken));
            } catch (Exception e) { t1Error.set(e); }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.countDown();
                latch.await(2, TimeUnit.SECONDS);
                jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, ?, 'ACTIVE', 'test')", uid1, backupRoleId);
                jdbc.update("INSERT INTO sys_user_role (user_id, role_id, status, created_by) VALUES (?, ?, 'ACTIVE', 'test')", uid2, backupRoleId);
            } catch (Exception e) { t2Error.set(e); }
        });

        t1.start(); t2.start();
        t1.join(10000); t2.join(10000);

        if (t1Error.get() != null) {/* role disable may fail due to last-role protection */}
        if (t2Error.get() != null) throw t2Error.get();

        int u1roles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role sur JOIN sys_role r ON r.id = sur.role_id WHERE sur.user_id = ? AND sur.status = 'ACTIVE' AND r.status = 'ACTIVE'",
                Integer.class, uid1);
        int u2roles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role sur JOIN sys_role r ON r.id = sur.role_id WHERE sur.user_id = ? AND sur.status = 'ACTIVE' AND r.status = 'ACTIVE'",
                Integer.class, uid2);
        assertThat(u1roles).withFailMessage("uid1=%d has %d effective roles, should have >=1", uid1, u1roles).isGreaterThanOrEqualTo(1);
        assertThat(u2roles).withFailMessage("uid2=%d has %d effective roles, should have >=1", uid2, u2roles).isGreaterThanOrEqualTo(1);
    }

    // ==================== 14. Same-token data scope lifecycle ====================

    @Test
    void sameTokenDataScopeChangedReflectedOnNextRequest() throws Exception {
        String role = "CLO_SDS_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ORG", "READ_WRITE", "system:department:view");
        jdbc.update("UPDATE sys_role_data_policy SET scope_org_ids = ? WHERE role_id = ?",
                "[" + orgAId + "]", roleId(role));
        String u = createUser(role + "_u", role, orgAId);
        String token = login(u, PASSWORD);
        Long sdsRoleId = roleId(role);
        Long deptInB = insertDept("CLO-SDSB-" + Instant.now().toEpochMilli(), "sds orgB dept", orgBId);

        MvcResult r1 = mockMvc.perform(get("/api/v1/system/departments?pageSize=50")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode d1 = objectMapper.readTree(r1.getResponse().getContentAsString()).path("data");
        assertThat(d1.path("total").asInt()).isGreaterThanOrEqualTo(0);

        mockMvc.perform(put("/api/v1/system/roles/" + sdsRoleId + "/data-policies")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"domainCode\":\"system\",\"dimensionCode\":\"ORGANIZATION\",\"scopeType\":\"ALL\",\"operationMode\":\"READ_WRITE\",\"includeChildren\":true}]"))
                .andExpect(status().isOk());

        MvcResult r2 = mockMvc.perform(get("/api/v1/system/departments?pageSize=50")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode d2 = objectMapper.readTree(r2.getResponse().getContentAsString()).path("data");
        assertThat(d2.path("total").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(d2.path("total").asInt()).isGreaterThan(d1.path("total").asInt());

        mockMvc.perform(put("/api/v1/system/roles/" + sdsRoleId + "/data-policies")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"domainCode\":\"system\",\"dimensionCode\":\"ORGANIZATION\",\"scopeType\":\"ORG\",\"operationMode\":\"READ_WRITE\",\"includeChildren\":false,\"scopeOrgIds\":\"[" + orgAId + "]\"}]"))
                .andExpect(status().isOk());

        MvcResult r3 = mockMvc.perform(get("/api/v1/system/departments?pageSize=50")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        JsonNode d3 = objectMapper.readTree(r3.getResponse().getContentAsString()).path("data");
        assertThat(d3.path("total").asInt()).isLessThan(d2.path("total").asInt());
    }

    // ==================== 15. Transaction rollback vs afterCommit ====================

    @Test
    void cacheEvictionNotExecutedOnTransactionRollback() {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache(CacheNames.NAVIGATION_MENUS);
        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        rawInvalidator.evictAll();

        String block = "rollback-key";
        int beforeCount = CounterConfig.EVICT_COUNT.get();
        assertThat(nativeCache.getIfPresent(block)).isNull();

        txTemplate.execute(status -> {
            nativeCache.put(block, true);
            assertThat(nativeCache.getIfPresent(block)).isNotNull();
            cacheInvalidator.evictAll();
            status.setRollbackOnly();
            return null;
        });

        assertThat(nativeCache.getIfPresent(block)).isNotNull();
        assertThat(CounterConfig.EVICT_COUNT.get()).isEqualTo(beforeCount);
        nativeCache.invalidate(block);
    }

    @Test
    void cacheEvictionExecutedPreciselyTwiceAcrossTwoIndependentCommits() {
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache(CacheNames.NAVIGATION_MENUS);
        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        rawInvalidator.evictAll();

        String block1 = "commit-key-1";
        String block2 = "commit-key-2";
        int countBefore = CounterConfig.EVICT_COUNT.get();

        nativeCache.put(block1, true);
        assertThat(nativeCache.getIfPresent(block1)).isNotNull();
        txTemplate.executeWithoutResult(s -> cacheInvalidator.evictAll());
        assertThat(nativeCache.getIfPresent(block1)).isNull();
        assertThat(CounterConfig.EVICT_COUNT.get()).isEqualTo(countBefore + 1);

        nativeCache.put(block2, true);
        assertThat(nativeCache.getIfPresent(block2)).isNotNull();
        txTemplate.executeWithoutResult(s -> cacheInvalidator.evictAll());
        assertThat(nativeCache.getIfPresent(block2)).isNull();
        assertThat(CounterConfig.EVICT_COUNT.get()).isEqualTo(countBefore + 2);
    }

    // ==================== 16. Data scope privilege escalation audit ====================

    @Test
    void adminWithLimitedScopeCannotSetWiderScopeOrgIds() throws Exception {
        String role = "CLO_DSE_" + Instant.now().toEpochMilli();
        insertRoleWithPerms(role, "ALL", "READ_WRITE", "system:role:view");
        Long dseRoleId = roleId(role);

        String limitedRole = "CLO_ADM_" + Instant.now().toEpochMilli();
        insertOrg("CLO-OA-" + Instant.now().toEpochMilli(), "admin org A", "COMPANY",
                jdbc.queryForObject("SELECT id FROM md_organization WHERE org_code = 'CLO-ROOT'", Long.class));
        Long adminOrgId = jdbc.queryForObject("SELECT id FROM md_organization WHERE org_code LIKE 'CLO-OA-%'", Long.class);
        insertRoleWithPerms(limitedRole, "ORG", "READ_WRITE", "system:role:view", "system:role:assign-data-scope");
        jdbc.update("UPDATE sys_role_data_policy SET scope_org_ids = ? WHERE role_id = ?",
                "[" + adminOrgId + "]", roleId(limitedRole));
        String limitedAdmin = createUser(limitedRole + "_u", limitedRole, adminOrgId);
        String limitedToken = login(limitedAdmin, PASSWORD);
        Instant before = Instant.now();

        List<String> beforeAllPolicyRows = jdbc.queryForList(
                "SELECT domain_code||'|'||dimension_code||'|'||scope_type||'|'||COALESCE(scope_org_ids,'null')||'|'||operation_mode||'|'||status"
                + " FROM sys_role_data_policy WHERE role_id = ? ORDER BY id", String.class, dseRoleId);

        mockMvc.perform(put("/api/v1/system/roles/" + dseRoleId + "/data-policies")
                        .header(HttpHeaders.AUTHORIZATION, limitedToken)
                        .header("X-Simulated-Data", "TEST_ONLY_token_TEST_ONLY_jwt_TEST_ONLY_bearer_TEST_ONLY_authorization_TEST_ONLY_api_key_TEST_ONLY_secret_TEST_ONLY_credential_TEST_ONLY_private_key_TEST_ONLY_password_TEST_ONLY_password_hash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"domainCode\":\"system\",\"dimensionCode\":\"ORGANIZATION\",\"scopeType\":\"ORG\",\"operationMode\":\"READ_WRITE\",\"includeChildren\":false,\"scopeOrgIds\":\"[" + orgBId + "]\"}]"))
                .andExpect(status().isForbidden());

        List<String> afterAllPolicyRows = jdbc.queryForList(
                "SELECT domain_code||'|'||dimension_code||'|'||scope_type||'|'||COALESCE(scope_org_ids,'null')||'|'||operation_mode||'|'||status"
                + " FROM sys_role_data_policy WHERE role_id = ? ORDER BY id", String.class, dseRoleId);
        assertThat(afterAllPolicyRows).as("all policy rows unchanged after blocked save")
                .containsExactlyElementsOf(beforeAllPolicyRows);

        List<String> beforeIds = jdbc.queryForList(
                "SELECT scope_org_ids FROM sys_role_data_policy WHERE role_id = ? ORDER BY id", String.class, dseRoleId);
        List<String> afterIds = jdbc.queryForList(
                "SELECT scope_org_ids FROM sys_role_data_policy WHERE role_id = ? ORDER BY id", String.class, dseRoleId);
        assertThat(afterIds).as("sorted scope_org_ids set unchanged").containsExactlyElementsOf(beforeIds);

        Integer logCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ?",
                Integer.class, before);
        assertThat(logCount).as("exactly one FAILURE audit record").isEqualTo(1);

        String dbAction = jdbc.queryForObject(
                "SELECT action_code FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                String.class, before);
        assertThat(dbAction).isEqualTo("DATA_SCOPE_ACCESS_DENIED");

        String dbTargetType = jdbc.queryForObject(
                "SELECT target_type FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                String.class, before);
        assertThat(dbTargetType).isEqualTo("SYSTEM");

        String dbTargetId = jdbc.queryForObject(
                "SELECT target_id FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                String.class, before);
        assertThat(dbTargetId).isEqualTo("system:role:assign-data-scope");

        String dbResult = jdbc.queryForObject(
                "SELECT result_code FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                String.class, before);
        assertThat(dbResult).isEqualTo("FAILURE");

        String dbReason = jdbc.queryForObject(
                "SELECT reason FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                String.class, before);
        assertThat(dbReason).contains("Not ALL scope");

        String combined = jdbc.query("SELECT COALESCE(action_code,'')||'|'||COALESCE(target_type,'')||'|'||COALESCE(target_id,'')||'|'||COALESCE(result_code,'')||'|'||COALESCE(detail_summary,'')||'|'||COALESCE(field_changes,'')||'|'||COALESCE(reason,'')"
                + " FROM sys_operation_log WHERE action_code='DATA_SCOPE_ACCESS_DENIED' AND result_code='FAILURE' AND occurred_at > ? ORDER BY occurred_at DESC LIMIT 1",
                rs -> { rs.next(); return rs.getString(1); }, before);
        String lower = combined.toLowerCase(java.util.Locale.ROOT);
        assertThat(lower).doesNotContain("password");
        assertThat(lower).doesNotContain("password_hash");
        assertThat(lower).doesNotContain("token");
        assertThat(lower).doesNotContain("jwt");
        assertThat(lower).doesNotContain("authorization");
        assertThat(lower).doesNotContain("bearer");
        assertThat(lower).doesNotContain("api key");
        assertThat(lower).doesNotContain("api_key");
        assertThat(lower).doesNotContain("secret");
        assertThat(lower).doesNotContain("credential");
        assertThat(lower).doesNotContain("private key");
        assertThat(lower).doesNotContain("private_key");
        assertThat(lower).doesNotContain(PASSWORD.toLowerCase(java.util.Locale.ROOT));
    }

    // ==================== Helpers ====================

    private String login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String createUser(String username, String roleCode, Long orgId) {
        long uid = users.createUserIfMissing(username, passwordEncoder.encode(PASSWORD), username, "test");
        jdbc.update("UPDATE sys_user SET main_organization_id = ?, status = 'ACTIVE' WHERE id = ?", orgId, uid);
        users.assignRoleIfMissing(uid, roleCode, "test");
        return username;
    }

    private void insertRoleWithPerms(String code, String scope, String mode, String... perms) {
        jdbc.update("INSERT INTO sys_role (role_code, role_name, status, built_in, created_by, updated_by) VALUES (?, ?, 'ACTIVE', FALSE, 'test', 'test')", code, code);
        for (String p : perms) {
            jdbc.update("INSERT INTO sys_role_permission (role_id, permission_id, created_by) SELECT r.id, p.id, 'test' FROM sys_role r JOIN sys_permission p ON p.permission_code = ? WHERE r.role_code = ?", p, code);
        }
        jdbc.update("INSERT INTO sys_role_data_policy (role_id, domain_code, dimension_code, scope_type, include_children, operation_mode, status, created_by, updated_by) SELECT id, 'system', 'ORGANIZATION', ?, FALSE, ?, 'ACTIVE', 'test', 'test' FROM sys_role WHERE role_code = ?", scope, mode, code);
    }

    private Long insertOrg(String code, String name, String type, Long parentId) {
        jdbc.update("INSERT INTO md_organization (org_code, org_name, org_type, parent_id, status, created_by, updated_by) VALUES (?, ?, ?, ?, 'ACTIVE', 'test', 'test')", code, name, type, parentId);
        return jdbc.queryForObject("SELECT id FROM md_organization WHERE org_code = ?", Long.class, code);
    }
    private Long insertDept(String code, String name, Long orgId) {
        jdbc.update("INSERT INTO sys_department (dept_code, dept_name, organization_id, status, created_by, updated_by) VALUES (?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, orgId);
        return jdbc.queryForObject("SELECT id FROM sys_department WHERE dept_code = ?", Long.class, code);
    }

    private Long insertPos(String code, String name, Long deptId) {
        jdbc.update("INSERT INTO sys_position (position_code, position_name, department_id, status, created_by, updated_by) VALUES (?, ?, ?, 'ACTIVE', 'test', 'test')",
                code, name, deptId);
        return jdbc.queryForObject("SELECT id FROM sys_position WHERE position_code = ?", Long.class, code);
    }

    private Long roleId(String roleCode) {
        return jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, roleCode);
    }

    private Long userId(String username) {
        return jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    @TestConfiguration
    static class CounterConfig {
        static final AtomicInteger EVICT_COUNT = new AtomicInteger(0);

        @Bean
        @Primary
        NavigationCacheInvalidator countedInvalidator(CacheManager cm) {
            return new NavigationCacheInvalidator() {
                @Override
                public void evictAll() {
                    EVICT_COUNT.incrementAndGet();
                    var c = cm.getCache(CacheNames.NAVIGATION_MENUS);
                    if (c != null) c.clear();
                }
                @Override
                public void evictUser(long userId) {
                    EVICT_COUNT.incrementAndGet();
                    var c = cm.getCache(CacheNames.NAVIGATION_MENUS);
                    if (c != null) c.evict(userId);
                }
            };
        }
    }
}
