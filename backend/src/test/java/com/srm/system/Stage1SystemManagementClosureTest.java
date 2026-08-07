package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.SrmPrincipal;
import com.srm.security.auth.UserAccountRepository;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.application.service.ApprovalCallbackRegistry;
import com.srm.system.application.service.ApprovalService;
import com.srm.system.application.service.IntegrationApplicationService;
import com.srm.system.infrastructure.persistence.MybatisAttachmentRepository;
import com.srm.system.infrastructure.persistence.MybatisDictionaryRepository;
import com.srm.system.infrastructure.persistence.MybatisIntegrationRepository;
import com.srm.system.infrastructure.persistence.MybatisNumberRuleRepository;
import com.srm.system.infrastructure.persistence.MybatisParameterRepository;
import com.srm.system.infrastructure.persistence.MybatisWorkflowQueryRepository;
import com.srm.system.infrastructure.persistence.entity.SysMessageEntity;
import com.srm.system.infrastructure.persistence.entity.SysWorkflowNodeEntity;
import com.srm.system.infrastructure.persistence.mapper.PositionMapper;
import com.srm.system.infrastructure.persistence.mapper.SysMessageMapper;
import com.srm.system.infrastructure.persistence.mapper.SysAttachmentVersionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 系统管理剩余功能专项测试：流程、字典参数、模板附件、消息、审计日志、接口与任务监控。
 * 每个测试独立构造数据（唯一编码），不依赖执行顺序，不共享残留数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1SystemManagementClosureTest {

    private static final String APPROVAL_BUSINESS_TYPE = "TEST_CLOSURE_APPROVAL";
    private static final AtomicBoolean CALLBACK_APPROVED = new AtomicBoolean(false);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApprovalService approvalService;
    @Autowired private ApprovalCallbackRegistry callbackRegistry;
    @Autowired private IntegrationApplicationService integrationService;
    @Autowired private MybatisIntegrationRepository integrationRepo;
    @Autowired private MybatisWorkflowQueryRepository workflowRepo;
    @Autowired private MybatisDictionaryRepository dictRepo;
    @Autowired private MybatisParameterRepository paramRepo;
    @Autowired private MybatisNumberRuleRepository numberRepo;
    @Autowired private MybatisAttachmentRepository attachRepo;
    @Autowired private SysMessageMapper messageMapper;
    @Autowired private PositionMapper positionMapper;
    @Autowired private SysAttachmentVersionMapper attachVersionMapper;

    private static String adminToken;
    private static String processAdminToken;
    private static Long processAdminId;
    private static String masterDataAdminToken;
    private static String auditorToken;
    private static String noPermissionToken;
    private static String positionUserToken;
    private static Long positionUserId;
    private static Long positionId;

    @BeforeAll
    void setup() throws Exception {
        callbackRegistry.register(new ApprovalCallbackRegistry.ApprovalCallback() {
            @Override public String businessType() { return APPROVAL_BUSINESS_TYPE; }
            @Override public void onApproved(String businessId) { CALLBACK_APPROVED.set(true); }
            @Override public void onRejected(String businessId) { CALLBACK_APPROVED.set(false); }
            @Override public void onWithdrawn(String businessId) { }
        });

        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        processAdminId = users.createUserIfMissing("smc_process_admin",
                passwordEncoder.encode("SmcProcessAdmin!2026"), "SMC Process Admin", "test");
        users.assignRoleIfMissing(processAdminId, "PROCESS_ADMIN", "test");
        processAdminToken = login("smc_process_admin", "SmcProcessAdmin!2026");

        long masterAdminId = users.createUserIfMissing("smc_master_admin",
                passwordEncoder.encode("SmcMasterAdmin!2026"), "SMC Master Admin", "test");
        users.assignRoleIfMissing(masterAdminId, "MASTER_DATA_ADMIN", "test");
        masterDataAdminToken = login("smc_master_admin", "SmcMasterAdmin!2026");

        long auditorId = users.createUserIfMissing("smc_auditor",
                passwordEncoder.encode("SmcAuditor!2026"), "SMC Auditor", "test");
        users.assignRoleIfMissing(auditorId, "INTERNAL_AUDITOR", "test");
        auditorToken = login("smc_auditor", "SmcAuditor!2026");

        long noPermId = users.createUserIfMissing("smc_noperm",
                passwordEncoder.encode("SmcNoPerm!2026"), "SMC No Perm", "test");
        noPermissionToken = login("smc_noperm", "SmcNoPerm!2026");

        positionUserId = users.createUserIfMissing("smc_position_user",
                passwordEncoder.encode("SmcPositionUser!2026"), "SMC Position User", "test");
        users.assignRoleIfMissing(positionUserId, "INTERNAL_USER", "test");
        positionUserToken = login("smc_position_user", "SmcPositionUser!2026");

        long orgId = insertOrganization("SMC-ORG-" + System.nanoTime(), "SMC Org", "COMPANY", null);
        long deptId = insertDepartment("SMC-DEPT-" + System.nanoTime(), "SMC Dept", orgId);
        positionId = insertPosition("SMC-POS-" + System.nanoTime(), "SMC Position", deptId);
        jdbc.update("UPDATE sys_user SET main_position_id = ? WHERE id = ?", positionId, positionUserId);
    }

    private void asAdmin() {
        var principal = new SrmPrincipal(1L, "stage0_admin", "", "Stage0 Admin", "ACTIVE",
                false, List.of("SUPER_ADMIN"), List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void clearAuth() { SecurityContextHolder.clearContext(); }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode().put("username", username)
                .put("password", password).toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    // ============================================================
    // 流程
    // ============================================================

    @Test @Order(1)
    void workflowKeywordAndStatusSearchReturnExpectedRows() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String codeA = "SMC_FLOW_A_" + suffix;
        String codeB = "SMC_FLOW_B_" + suffix;
        createWorkflow(codeA, "Keyword flow " + suffix, "SYSTEM_PARAMETER", null);
        createWorkflow(codeB, "Other flow " + suffix, "SYSTEM_PARAMETER", null);

        MvcResult byKeyword = mockMvc.perform(get("/api/v1/system/workflows?page=1&pageSize=100&keyword=" + suffix)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String body = byKeyword.getResponse().getContentAsString();
        assertThat(body).contains(codeA).contains(codeB);

        MvcResult byStatus = mockMvc.perform(get("/api/v1/system/workflows?page=1&pageSize=100&status=DRAFT&keyword=" + codeA)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String statusBody = byStatus.getResponse().getContentAsString();
        assertThat(statusBody).contains(codeA);
        assertThat(statusBody).doesNotContain(codeB);
    }

    @Test @Order(2)
    void workflowNodeValidationRejectsDuplicateCodesBlankNamesAndBadTypes() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"SMC_BAD_DUP_" + suffix + "\",\"processName\":\"dup\",\"businessType\":\"SYSTEM_PARAMETER\","
                                + "\"nodes\":[{\"nodeCode\":\"N1\",\"nodeName\":\"a\",\"nodeType\":\"APPROVAL\",\"sortOrder\":1,\"assigneeType\":\"USER\",\"assigneeValue\":\"1\"},"
                                + "{\"nodeCode\":\"N1\",\"nodeName\":\"b\",\"nodeType\":\"APPROVAL\",\"sortOrder\":2,\"assigneeType\":\"USER\",\"assigneeValue\":\"1\"}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"SMC_BAD_BLANK_" + suffix + "\",\"processName\":\"blank\",\"businessType\":\"SYSTEM_PARAMETER\","
                                + "\"nodes\":[{\"nodeCode\":\"N1\",\"nodeName\":\" \",\"nodeType\":\"APPROVAL\",\"sortOrder\":1,\"assigneeType\":\"USER\",\"assigneeValue\":\"1\"}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"SMC_BAD_TYPE_" + suffix + "\",\"processName\":\"type\",\"businessType\":\"SYSTEM_PARAMETER\","
                                + "\"nodes\":[{\"nodeCode\":\"N1\",\"nodeName\":\"a\",\"nodeType\":\"PARALLEL\",\"sortOrder\":1,\"assigneeType\":\"USER\",\"assigneeValue\":\"1\"}]}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"SMC_BAD_ASSIGNEE_" + suffix + "\",\"processName\":\"assignee\",\"businessType\":\"SYSTEM_PARAMETER\","
                                + "\"nodes\":[{\"nodeCode\":\"N1\",\"nodeName\":\"a\",\"nodeType\":\"APPROVAL\",\"sortOrder\":1,\"assigneeType\":\"DEPARTMENT\",\"assigneeValue\":\"1\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(3)
    void publishRejectsUnresolvableApproverAndEmptyNodes() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long flowId = createWorkflow("SMC_UNRESOLVED_" + suffix, "Unresolvable", "SYSTEM_PARAMETER",
                List.of(node("N1", "Bad approver", "USER", "999999", 48)));

        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());

        long emptyId = createWorkflow("SMC_EMPTY_" + suffix, "Empty", "SYSTEM_PARAMETER", List.of());
        mockMvc.perform(post("/api/v1/system/workflows/" + emptyId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(4)
    void publishedVersionFreezesAndNextVersionIsDraft() throws Exception {
        String code = "SMC_VERSION_" + System.nanoTime();
        long v1 = createWorkflow(code, "Version flow", "SYSTEM_PARAMETER",
                List.of(node("N1", "Approver", "USER", "1", 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + v1 + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/system/workflows/" + v1)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processName\":\"hacked\",\"nodes\":[]}"))
                .andExpect(status().isConflict());

        MvcResult created = mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"" + code + "\",\"processName\":\"Version flow\",\"businessType\":\"SYSTEM_PARAMETER\","
                                + "\"nodes\":[{\"nodeCode\":\"N1\",\"nodeName\":\"Approver\",\"nodeType\":\"APPROVAL\",\"sortOrder\":1,\"assigneeType\":\"USER\",\"assigneeValue\":\"1\",\"durationHours\":48}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitionVersion").value(2))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long v2 = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/system/workflows/" + v1)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/system/workflows/" + v2)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test @Order(5)
    void retireMarksPublishedWorkflowRetired() throws Exception {
        String code = "SMC_RETIRE_" + System.nanoTime();
        long flowId = createWorkflow(code, "Retire flow", "SYSTEM_PARAMETER",
                List.of(node("N1", "Approver", "USER", "1", 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/retire")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/workflows/" + flowId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"));
    }

    @Test @Order(6)
    void positionAssigneeResolvesRealUsersIntoTasks() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long flowId = createWorkflow("SMC_POS_FLOW_" + suffix, "Position flow",
                APPROVAL_BUSINESS_TYPE, List.of(node("N1", "Position approver", "POSITION",
                        String.valueOf(positionId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        asAdmin();
        Object instance;
        try {
            instance = approvalService.submit(APPROVAL_BUSINESS_TYPE, "SMC-POS-BIZ-" + suffix, "Position flow approval");
        } finally {
            clearAuth();
        }
        long instanceId = (long) instance.getClass().getMethod("getId").invoke(instance);

        MvcResult tasks = mockMvc.perform(get("/api/v1/tasks/my?page=1&pageSize=20&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, positionUserToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(tasks.getResponse().getContentAsString()).contains("Position flow approval");

        mockMvc.perform(get("/api/v1/system/approvals/" + instanceId + "/nodes")
                        .header(HttpHeaders.AUTHORIZATION, positionUserToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, positionUserToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"no permission\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/approvals/" + instanceId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(7)
    void processAdminCanApproveRejectAndWithdrawFlow() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long flowId = createWorkflow("SMC_PROC_FLOW_" + suffix, "Process admin flow",
                APPROVAL_BUSINESS_TYPE, List.of(node("N1", "Process approver", "USER",
                        String.valueOf(processAdminId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        asAdmin();
        Object instance;
        try {
            instance = approvalService.submit(APPROVAL_BUSINESS_TYPE, "SMC-PROC-BIZ-" + suffix, "Process admin approval");
        } finally {
            clearAuth();
        }
        long instanceId = (long) instance.getClass().getMethod("getId").invoke(instance);

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, processAdminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"同意\"}"))
                .andExpect(status().isOk());
        assertThat(CALLBACK_APPROVED).isTrue();
        mockMvc.perform(get("/api/v1/system/approvals/" + instanceId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        CALLBACK_APPROVED.set(true);
        long rejectFlowId = createWorkflow("SMC_PROC_REJECT_" + suffix, "Process admin reject",
                APPROVAL_BUSINESS_TYPE, List.of(node("N1", "Reject approver", "USER",
                        String.valueOf(processAdminId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + rejectFlowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        asAdmin();
        Object rejectInstance;
        try {
            rejectInstance = approvalService.submit(APPROVAL_BUSINESS_TYPE, "SMC-PROC-REJ-" + suffix, "Reject me");
        } finally {
            clearAuth();
        }
        long rejectId = (long) rejectInstance.getClass().getMethod("getId").invoke(rejectInstance);
        mockMvc.perform(post("/api/v1/system/approvals/" + rejectId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, processAdminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"驳回\"}"))
                .andExpect(status().isOk());
        assertThat(CALLBACK_APPROVED).isFalse();

        long withdrawFlowId = createWorkflow("SMC_PROC_WITHDRAW_" + suffix, "Process admin withdraw",
                APPROVAL_BUSINESS_TYPE, List.of(node("N1", "Withdraw approver", "USER",
                        String.valueOf(processAdminId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + withdrawFlowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        asAdmin();
        Object withdrawInstance;
        try {
            withdrawInstance = approvalService.submit(APPROVAL_BUSINESS_TYPE, "SMC-PROC-WD-" + suffix, "Withdraw me");
        } finally {
            clearAuth();
        }
        long withdrawId = (long) withdrawInstance.getClass().getMethod("getId").invoke(withdrawInstance);
        mockMvc.perform(post("/api/v1/system/approvals/" + withdrawId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/approvals/" + withdrawId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    // ============================================================
    // 字典参数
    // ============================================================

    @Test @Order(8)
    void dictionaryKeywordStatusFilterAndInactiveConstraints() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "SMC_DICT_" + suffix;
        long dictId = dictRepo.createDict(code, "SMC dictionary " + suffix, "test").getId();
        dictRepo.createItem(dictId, "ITEM_A", "Item A", 10);

        MvcResult filtered = mockMvc.perform(get("/api/v1/system/dictionaries?keyword=" + suffix + "&status=ACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(filtered.getResponse().getContentAsString()).contains(code);

        mockMvc.perform(get("/api/v1/system/dictionaries?status=INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/dictionaries/" + dictId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/dictionaries/" + dictId + "/items")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemCode\":\"ITEM_B\",\"itemName\":\"Item B\",\"sortOrder\":20}"))
                .andExpect(status().isConflict());

        long itemId = jdbc.queryForObject(
                "SELECT id FROM sys_dictionary_item WHERE dict_id = ? AND item_code = 'ITEM_A'",
                Long.class, dictId);
        mockMvc.perform(post("/api/v1/system/dictionaries/" + dictId + "/items/" + itemId + "/disable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/system/dictionaries/" + dictId + "/items/" + itemId + "/enable")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test @Order(9)
    void parameterUpdateAndTypeValidation() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "SMC_PARAM_" + suffix;
        long paramId = paramRepo.createParam(code, "Threshold", "DECIMAL", "0.5", null, true, "t").getId();

        mockMvc.perform(put("/api/v1/system/parameters/" + paramId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramName\":\"New threshold\",\"paramType\":\"INTEGER\",\"defaultValue\":\"10\",\"approvalRequired\":true,\"description\":\"updated\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramValue\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramValue\":\"25\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/system/parameters")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramCode\":\"SMC_BAD_TYPE_" + suffix + "\",\"paramName\":\"bad\",\"paramType\":\"JSON\",\"defaultValue\":\"{}\",\"approvalRequired\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(10)
    void numberRulePreviewDoesNotConsumeSequence() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "SMC_NUM_" + suffix;
        numberRepo.createRule(code, "SMC number", "ORDER", "SMC", "yyyyMMdd", 4, "DAY", false);

        MvcResult p1 = mockMvc.perform(post("/api/v1/system/number-rules/preview")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String preview1 = objectMapper.readTree(p1.getResponse().getContentAsString()).path("data").asText();

        MvcResult p2 = mockMvc.perform(post("/api/v1/system/number-rules/preview")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"" + code + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String preview2 = objectMapper.readTree(p2.getResponse().getContentAsString()).path("data").asText();
        assertThat(preview1).isEqualTo(preview2).startsWith("SMC");

        MvcResult generated = mockMvc.perform(post("/api/v1/system/number-rules/generate?ruleCode=" + code)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String gen = objectMapper.readTree(generated.getResponse().getContentAsString()).path("data").asText();
        assertThat(gen).isEqualTo(preview1);

        mockMvc.perform(post("/api/v1/system/number-rules/preview")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"SMC_MISSING_" + suffix + "\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/system/number-rules")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"SMC_BAD_NUM_" + suffix + "\",\"ruleName\":\"bad\",\"objectType\":\"X\",\"prefix\":\"X\",\"serialLength\":99,\"resetCycle\":\"DAY\"}"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // 模板附件
    // ============================================================

    @Test @Order(11)
    void templateDraftEditAndPublishedImmutability() throws Exception {
        String code = "SMC_TMPL_" + System.nanoTime();
        MvcResult created = mockMvc.perform(post("/api/v1/system/document-templates")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateCode\":\"" + code + "\",\"templateName\":\"SMC template\",\"purpose\":\"import\",\"domainCode\":\"masterdata\"}"))
                .andExpect(status().isOk()).andReturn();
        long templateId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/system/document-templates/" + templateId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateName\":\"SMC renamed\",\"purpose\":\"import v2\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/document-templates/" + templateId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("SMC renamed"));

        MockMultipartFile pdf = new MockMultipartFile("file", "smc.pdf", "application/pdf",
                "%PDF-1.4\nsmc-template".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        MvcResult uploaded = mockMvc.perform(multipart("/api/v1/system/attachments")
                        .file(pdf)
                        .param("ownerType", "DOCUMENT_TEMPLATE")
                        .param("ownerId", String.valueOf(templateId))
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        long attachmentId = objectMapper.readTree(uploaded.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(put("/api/v1/system/document-templates/" + templateId + "/attachment")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attachmentId\":" + attachmentId + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/system/document-templates/" + templateId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/system/document-templates/" + templateId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateName\":\"hacked\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/system/document-templates/" + templateId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(12)
    void attachmentReplaceKeepsVersionRecordsAndDeletedDownloadReturns404() throws Exception {
        byte[] v1 = "%PDF-1.4\nversion-one".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var att = attachRepo.upload("replace.pdf", "application/pdf", v1, "USER", "1");
        MockMultipartFile v2File = new MockMultipartFile("file", "replace-v2.pdf", "application/pdf",
                "%PDF-1.4\nversion-two".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        MvcResult replaced = mockMvc.perform(multipart("/api/v1/system/attachments/" + att.getId() + "/replace")
                        .file(v2File)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(replaced.getResponse().getContentAsString())
                .path("data").path("originalName").asText()).isEqualTo("replace-v2.pdf");
        assertThat(attachVersionMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                com.srm.system.infrastructure.persistence.entity.SysAttachmentVersionEntity>()
                .eq(com.srm.system.infrastructure.persistence.entity.SysAttachmentVersionEntity::getAttachmentId, att.getId())))
                .isEqualTo(2);

        mockMvc.perform(get("/api/v1/system/attachments/" + att.getId() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/system/attachments/" + att.getId())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/attachments/" + att.getId() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test @Order(13)
    void masterDataAdminUsesTemplateAttachmentAndBatchPages() throws Exception {
        mockMvc.perform(get("/api/v1/system/document-templates?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/attachments/by-owner?ownerType=DOCUMENT_TEMPLATE&ownerId=1")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/batch-jobs?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/system/batch-jobs/import-template?objectType=UNIT")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/system/workflows?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, masterDataAdminToken))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // 消息
    // ============================================================

    @Test @Order(14)
    void messageUnreadCountMatchesUnreadListAndOwnershipIsEnforced() throws Exception {
        asAdmin();
        long recipientId = 1L;
        String key = "SMC-MSG-" + System.nanoTime();
        SysMessageEntity message = new SysMessageEntity();
        message.setRecipientId(recipientId);
        message.setTitle("SMC unread message");
        message.setContent("consistency check");
        message.setMessageType("INFO");
        message.setStatus("UNREAD");
        message.setSourceType("SMC_TEST");
        message.setSourceId(key);
        message.setIdempotencyKey(key);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        clearAuth();

        MvcResult unread = mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        long unreadCount = objectMapper.readTree(unread.getResponse().getContentAsString()).path("data").asLong();

        MvcResult listResult = mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=500&status=UNREAD")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(listResult.getResponse().getContentAsString()).path("data").path("items");
        long listedUnread = 0;
        for (JsonNode it : items) {
            if (it.path("status").asText().equals("UNREAD")) listedUnread++;
        }
        assertThat(listedUnread).isEqualTo(unreadCount);

        mockMvc.perform(post("/api/v1/messages/" + message.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, processAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/messages/" + message.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/messages/read-all")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    // ============================================================
    // 审计日志
    // ============================================================

    @Test @Order(15)
    void auditFiltersDetailAndWriteTrails() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String dictCode = "SMC_AUDIT_DICT_" + suffix;
        MvcResult created = mockMvc.perform(post("/api/v1/system/dictionaries")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"" + dictCode + "\",\"dictName\":\"audit dict\",\"description\":\"t\"}"))
                .andExpect(status().isOk()).andReturn();
        long dictId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult byAction = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=50&actionCode=DICTIONARY_CREATED&operatorName=stage0_admin&resultCode=SUCCESS")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String actionBody = byAction.getResponse().getContentAsString();
        assertThat(actionBody).contains(dictCode);

        MvcResult byTarget = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=50&targetType=DICTIONARY&targetId=" + dictId + "&resultCode=SUCCESS")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(byTarget.getResponse().getContentAsString()).contains(dictCode);

        MvcResult byTime = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=50&from=2026-01-01T00:00:00&to=2027-01-01T00:00:00")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(byTime.getResponse().getContentAsString())
                .path("data").path("total").asLong()).isPositive();

        JsonNode items = objectMapper.readTree(actionBody).path("data").path("items");
        long firstId = -1;
        String firstTraceId = null;
        for (JsonNode it : items) {
            if (it.path("targetId").asText().equals(String.valueOf(dictId))) {
                firstId = it.path("id").asLong();
                firstTraceId = it.path("traceId").asText();
                break;
            }
        }
        assertThat(firstId).isPositive();

        MvcResult detail = mockMvc.perform(get("/api/v1/system/audit-logs/" + firstId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode detailNode = objectMapper.readTree(detail.getResponse().getContentAsString()).path("data");
        assertThat(detailNode.path("actionCode").asText()).isEqualTo("DICTIONARY_CREATED");
        assertThat(detailNode.path("occurredAt").asText()).isNotBlank();
        assertThat(detailNode.path("fieldChanges").isNull()).isFalse();
        assertThat(detailNode.path("afterHash").isNull()).isFalse();
        assertThat(detailNode.path("traceId").asText()).isEqualTo(firstTraceId);

        MvcResult byTrace = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=50&traceId=" + firstTraceId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(byTrace.getResponse().getContentAsString()).contains(firstTraceId);

        MvcResult missing = mockMvc.perform(get("/api/v1/system/audit-logs/999999999")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound()).andReturn();
        assertThat(missing.getResponse().getStatus()).isEqualTo(404);
    }

    @Test @Order(16)
    void auditorReadsButAllWritesReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isOk());
        MvcResult existing = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=1")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode firstItem = objectMapper.readTree(existing.getResponse().getContentAsString())
                .path("data").path("items").get(0);
        long firstId = firstItem != null ? firstItem.path("id").asLong() : 1L;
        mockMvc.perform(get("/api/v1/system/audit-logs/" + firstId)
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/dictionaries")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"SMC_EVIL\",\"dictName\":\"evil\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/parameters")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramCode\":\"SMC_EVIL_P\",\"paramName\":\"evil\",\"paramType\":\"STRING\",\"approvalRequired\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/number-rules")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleCode\":\"SMC_EVIL_R\",\"ruleName\":\"evil\",\"objectType\":\"X\",\"prefix\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/document-templates")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateCode\":\"SMC_EVIL_T\",\"templateName\":\"evil\",\"domainCode\":\"masterdata\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"SMC_EVIL_W\",\"processName\":\"evil\",\"businessType\":\"SYSTEM_PARAMETER\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/outbox-events/1/retry")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/messages/read-all")
                        .header(HttpHeaders.AUTHORIZATION, auditorToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(17)
    void noPermissionUserGets403OnAllSystemManagementApis() throws Exception {
        mockMvc.perform(get("/api/v1/system/workflows?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, noPermissionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/dictionaries")
                        .header(HttpHeaders.AUTHORIZATION, noPermissionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, noPermissionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system/inbox-events?page=1&size=10")
                        .header(HttpHeaders.AUTHORIZATION, noPermissionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, noPermissionToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(18)
    void parameterVersionWriteTrailsAppearInAuditWithoutSensitiveData() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "SMC_TRAIL_" + suffix;
        long paramId = paramRepo.createParam(code, "Trail", "STRING", "a", null, true, "t").getId();
        mockMvc.perform(put("/api/v1/system/parameters/" + paramId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramName\":\"Trail updated\",\"paramType\":\"STRING\",\"defaultValue\":\"a\",\"approvalRequired\":true,\"description\":\"u\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramValue\":\"b\"}"))
                .andExpect(status().isOk());

        MvcResult auditResult = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=200&targetType=PARAMETER")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        String auditBody = auditResult.getResponse().getContentAsString();
        assertThat(auditBody).contains("PARAMETER_UPDATED");

        MvcResult versionAudit = mockMvc.perform(get("/api/v1/system/audit-logs?page=1&pageSize=200&targetType=PARAMETER_VERSION")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(versionAudit.getResponse().getContentAsString()).contains("PARAMETER_VERSION_CREATED");
        assertThat(auditBody).doesNotContain("password").doesNotContain("secret");
    }

    // ============================================================
    // 接口与任务监控
    // ============================================================

    @Test @Order(19)
    void inboxDetailAttemptsFiltersAndDeadEventNotRetryable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        asAdmin();
        var failed = integrationService.receiveInbox(new InboxEventRequest(
                "SMC-INBOX-" + suffix, "ERP", "NO_SUCH_OBJECT", "X-" + suffix, 1L, "{}"));
        clearAuth();
        assertThat(failed.status()).isEqualTo("FAILED");

        MvcResult detail = mockMvc.perform(get("/api/v1/system/inbox-events/" + failed.id())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("SMC-INBOX-" + suffix))
                .andReturn();
        assertThat(detail.getResponse().getContentAsString()).contains("NO_SUCH_OBJECT");

        MvcResult attempts = mockMvc.perform(get("/api/v1/system/inbox-events/" + failed.id() + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(attempts.getResponse().getContentAsString()).contains("INBOX_EVENT_RECEIVED");

        MvcResult filtered = mockMvc.perform(get("/api/v1/system/inbox-events?page=1&size=50&sourceSystem=ERP&objectType=NO_SUCH_OBJECT&status=FAILED")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(filtered.getResponse().getContentAsString()).contains("SMC-INBOX-" + suffix);

        mockMvc.perform(post("/api/v1/system/inbox-events/999999999/retry")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());

        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/v1/system/inbox-events/" + failed.id() + "/retry")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/system/inbox-events/" + failed.id())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEAD"));
        mockMvc.perform(post("/api/v1/system/inbox-events/" + failed.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test @Order(20)
    void outboxDetailAttemptsAndTerminalStateNotRetryable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        asAdmin();
        var outbox = integrationService.createOutbox(new OutboxEventRequest(
                "SMC-OUTBOX-" + suffix, "MATERIAL", "M-" + suffix, 1L, "CREATED", "smc", "trace-smc"));
        integrationRepo.claimOutbox(outbox.id(), LocalDateTime.now());
        integrationRepo.markOutboxFailed(outbox.id(), "timeout", LocalDateTime.now().plusMinutes(1));
        clearAuth();

        MvcResult detail = mockMvc.perform(get("/api/v1/system/outbox-events/" + outbox.id())
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("SMC-OUTBOX-" + suffix))
                .andReturn();
        assertThat(detail.getResponse().getContentAsString()).contains("MATERIAL");

        MvcResult attempts = mockMvc.perform(get("/api/v1/system/outbox-events/" + outbox.id() + "/attempts")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(attempts.getResponse().getContentAsString()).contains("OUTBOX_EVENT_CREATED");

        MvcResult filtered = mockMvc.perform(get("/api/v1/system/outbox-events?page=1&size=50&eventType=CREATED&objectType=MATERIAL&status=FAILED")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(filtered.getResponse().getContentAsString()).contains("SMC-OUTBOX-" + suffix);

        mockMvc.perform(post("/api/v1/system/outbox-events/" + outbox.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        asAdmin();
        integrationRepo.claimOutbox(outbox.id(), LocalDateTime.now());
        integrationRepo.markOutboxPublished(outbox.id());
        clearAuth();
        mockMvc.perform(post("/api/v1/system/outbox-events/" + outbox.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test @Order(21)
    void inboxDuplicateAndStaleVersionHandlingAcrossEndpoints() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        asAdmin();
        var first = integrationService.receiveInbox(new InboxEventRequest(
                "SMC-DUP-" + suffix, "ERP", "MATERIAL", "SM-" + suffix, 1L,
                "{\"materialCode\":\"SM-" + suffix + "\",\"materialName\":\"First\",\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}"));
        var duplicate = integrationService.receiveInbox(new InboxEventRequest(
                "SMC-DUP-" + suffix, "ERP", "MATERIAL", "SM-" + suffix, 1L,
                "{\"materialCode\":\"SM-" + suffix + "\",\"materialName\":\"Dup\",\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}"));
        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(duplicate.status()).isEqualTo("PROCESSED");
        assertThat(integrationService.getInbox(first.id()).eventId()).isEqualTo("SMC-DUP-" + suffix);

        var stale = integrationService.receiveInbox(new InboxEventRequest(
                "SMC-OLD-" + suffix, "ERP", "MATERIAL", "SM-" + suffix, 0L,
                "{\"materialCode\":\"SM-" + suffix + "\",\"materialName\":\"Old\",\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}"));
        clearAuth();
        assertThat(stale.status()).isEqualTo("IGNORED_STALE");
    }

    @Test @Order(22)
    void outboxConcurrentClaimAllowsOnlyOneProcessor() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        asAdmin();
        var outbox = integrationService.createOutbox(new OutboxEventRequest(
                "SMC-CLAIM-" + suffix, "MATERIAL", "M-" + suffix, 1L, "CREATED", "claim", "t"));
        clearAuth();
        assertThat(integrationRepo.claimOutbox(outbox.id(), LocalDateTime.now())).isTrue();
        assertThat(integrationRepo.claimOutbox(outbox.id(), LocalDateTime.now())).isFalse();
    }

    @Test @Order(23)
    void roleApproverPublishRequiresActiveUsers() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        String emptyRole = "SMC_EMPTY_ROLE_" + suffix;
        insertRole(emptyRole, "Empty role");
        long emptyFlow = createWorkflow("SMC_ROLE_EMPTY_" + suffix, "Role empty", "SYSTEM_PARAMETER",
                List.of(node("N1", "Role approver", "ROLE", emptyRole, 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + emptyFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());

        String disabledRole = "SMC_DISABLED_ROLE_" + suffix;
        insertRole(disabledRole, "Disabled users role");
        long disabledUser = users.createUserIfMissing("smc_role_disabled_" + suffix,
                passwordEncoder.encode("SmcDisabled!2026"), "SMC Disabled", "test");
        users.assignRoleIfMissing(disabledUser, disabledRole, "test");
        jdbc.update("UPDATE sys_user SET status = 'INACTIVE' WHERE id = ?", disabledUser);
        long disabledFlow = createWorkflow("SMC_ROLE_DISABLED_" + suffix, "Role disabled", "SYSTEM_PARAMETER",
                List.of(node("N1", "Role approver", "ROLE", disabledRole, 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + disabledFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());

        String activeRole = "SMC_ACTIVE_ROLE_" + suffix;
        insertRole(activeRole, "Active users role");
        long activeUser = users.createUserIfMissing("smc_role_active_" + suffix,
                passwordEncoder.encode("SmcActive!2026"), "SMC Active", "test");
        users.assignRoleIfMissing(activeUser, activeRole, "test");
        long activeFlow = createWorkflow("SMC_ROLE_ACTIVE_" + suffix, "Role active", "SYSTEM_PARAMETER",
                List.of(node("N1", "Role approver", "ROLE", activeRole, 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + activeFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(24)
    void positionApproverPublishRequiresActiveUsers() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        long orgId = insertOrganization("SMC-PO-ORG-" + suffix, "PO Org", "COMPANY", null);
        long deptId = insertDepartment("SMC-PO-DEPT-" + suffix, "PO Dept", orgId);

        long emptyPositionId = insertPosition("SMC-EMPTY-POS-" + suffix, "Empty position", deptId);
        long emptyFlow = createWorkflow("SMC_POS_EMPTY_" + suffix, "Pos empty", "SYSTEM_PARAMETER",
                List.of(node("N1", "Pos approver", "POSITION", String.valueOf(emptyPositionId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + emptyFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());

        long disabledPositionId = insertPosition("SMC-DISABLED-POS-" + suffix, "Disabled position", deptId);
        long disabledUser = users.createUserIfMissing("smc_pos_disabled_" + suffix,
                passwordEncoder.encode("SmcPosDisabled!2026"), "SMC Pos Disabled", "test");
        jdbc.update("UPDATE sys_user SET main_position_id = ?, status = 'INACTIVE' WHERE id = ?",
                disabledPositionId, disabledUser);
        long disabledFlow = createWorkflow("SMC_POS_DISABLED_" + suffix, "Pos disabled", "SYSTEM_PARAMETER",
                List.of(node("N1", "Pos approver", "POSITION", String.valueOf(disabledPositionId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + disabledFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());

        long activePositionId = insertPosition("SMC-ACTIVE-POS-" + suffix, "Active position", deptId);
        long activeUser = users.createUserIfMissing("smc_pos_active_" + suffix,
                passwordEncoder.encode("SmcPosActive!2026"), "SMC Pos Active", "test");
        jdbc.update("UPDATE sys_user SET main_position_id = ? WHERE id = ?", activePositionId, activeUser);
        long activeFlow = createWorkflow("SMC_POS_ACTIVE_" + suffix, "Pos active", "SYSTEM_PARAMETER",
                List.of(node("N1", "Pos approver", "POSITION", String.valueOf(activePositionId), 48)));
        mockMvc.perform(post("/api/v1/system/workflows/" + activeFlow + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(25)
    void roleWithOnlySubmitterPublishesButSubmitFailsBySelfApprovalProtection() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String submitterRole = "SMC_SUBMITTER_ROLE_" + suffix;
        insertRole(submitterRole, "Submitter only role");
        try {
            users.assignRoleIfMissing(1L, submitterRole, "test");

            long flowId = createWorkflow("SMC_SUBMITTER_ROLE_" + suffix, "Submitter role flow",
                    APPROVAL_BUSINESS_TYPE, List.of(node("N1", "Self approver", "ROLE", submitterRole, 48)));
            mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                            .header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isOk());

            asAdmin();
            assertThatThrownBy(() -> approvalService.submit(APPROVAL_BUSINESS_TYPE,
                    "SMC-SELF-BIZ-" + suffix, "Self approval"))
                    .isInstanceOf(com.srm.common.exception.BusinessException.class)
                    .hasMessageContaining("no eligible approver");
        } finally {
            clearAuth();
            jdbc.update("""
                    DELETE FROM sys_user_role
                     WHERE user_id = 1
                       AND role_id = (SELECT id FROM sys_role WHERE role_code = ?)
                    """, submitterRole);
        }
    }

    // ============================================================
    // helpers
    // ============================================================

    private long createWorkflow(String code, String name, String businessType,
                                List<SysWorkflowNodeEntity> nodes) throws Exception {
        String nodesJson = nodes == null || nodes.isEmpty() ? "[]"
                : "[" + nodes.stream().map(this::nodeJson).reduce((a, b) -> a + "," + b).orElse("") + "]";
        MvcResult r = mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processCode\":\"" + code + "\",\"processName\":\"" + name
                                + "\",\"businessType\":\"" + businessType
                                + "\",\"description\":\"smc\",\"nodes\":" + nodesJson + "}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private SysWorkflowNodeEntity node(String code, String name, String assigneeType,
                                      String assigneeValue, Integer durationHours) {
        var n = new SysWorkflowNodeEntity();
        n.setNodeCode(code); n.setNodeName(name); n.setNodeType("APPROVAL");
        n.setSortOrder(1); n.setAssigneeType(assigneeType); n.setAssigneeValue(assigneeValue);
        n.setDurationHours(durationHours);
        return n;
    }

    private String nodeJson(SysWorkflowNodeEntity n) {
        return "{\"nodeCode\":\"" + n.getNodeCode() + "\",\"nodeName\":\"" + n.getNodeName()
                + "\",\"nodeType\":\"APPROVAL\",\"sortOrder\":" + n.getSortOrder()
                + ",\"assigneeType\":\"" + n.getAssigneeType()
                + "\",\"assigneeValue\":\"" + n.getAssigneeValue()
                + "\",\"durationHours\":" + (n.getDurationHours() == null ? 48 : n.getDurationHours()) + "}";
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
}
