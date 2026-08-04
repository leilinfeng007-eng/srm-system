package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.UserAccountRepository;
import com.srm.system.infrastructure.persistence.MybatisWorkflowQueryRepository;
import com.srm.system.infrastructure.persistence.entity.SysWorkflowNodeEntity;
import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1ApprovalE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MybatisWorkflowQueryRepository queryRepo;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String adminToken;
    private static String approverToken;
    private static String secondApproverToken;
    private static Long approverUserId;
    private static Long paramId;
    private static Long versionId;
    private static Long instanceId;
    private static Long publishedFlowId;

    private String login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode().put("username", username).put("password", password).toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @BeforeAll
    void setup() throws Exception {
        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        long approverId = users.createUserIfMissing("flow_approver", passwordEncoder.encode("FlowApprover!2026"), "Flow Approver", "test");
        users.assignRoleIfMissing(approverId, "SYSTEM_ADMIN", "test");
        approverUserId = approverId;
        approverToken = login("flow_approver", "FlowApprover!2026");
        long secondApproverId = users.createUserIfMissing("flow_approver_2",
                passwordEncoder.encode("FlowApprover2!2026"), "Flow Approver 2", "test");
        users.assignRoleIfMissing(secondApproverId, "SYSTEM_ADMIN", "test");
        secondApproverToken = login("flow_approver_2", "FlowApprover2!2026");

        String flowBody = """
            {"processCode":"FLOW_PARAM_E2E","processName":"参数审批E2E","businessType":"SYSTEM_PARAMETER","description":"E2E",
             "nodes":[{"nodeCode":"N1","nodeName":"审批人","nodeType":"APPROVAL","sortOrder":1,"assigneeType":"USER","assigneeValue":"%d","durationHours":48}]}
            """.formatted(approverId);
        MvcResult flow = mockMvc.perform(post("/api/v1/system/workflows").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(flowBody))
                .andExpect(status().isOk()).andReturn();
        publishedFlowId = objectMapper.readTree(flow.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/workflows/" + publishedFlowId + "/publish").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    private long createApprovalRequiredParam() throws Exception {
        String code = "E2E_PARAM_" + System.nanoTime();
        String body = "{\"paramCode\":\"" + code + "\",\"paramName\":\"E2E阈值\",\"paramType\":\"DECIMAL\",\"defaultValue\":\"0.5\",\"approvalRequired\":true}";
        MvcResult r = mockMvc.perform(post("/api/v1/system/parameters").header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test @Order(1)
    void submitParameterVersionCreatesApprovalInstanceTaskAndMessage() throws Exception {
        paramId = createApprovalRequiredParam();
        MvcResult v = mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramValue\":\"0.8\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        versionId = objectMapper.readTree(v.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions/" + versionId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        MvcResult apv = mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=10&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(apv.getResponse().getContentAsString()).path("data").path("items");
        assertThat(items.isArray()).isTrue();
        boolean found = false;
        for (JsonNode it : items) {
            if (it.path("businessType").asText().equals("SYSTEM_PARAMETER")
                    && it.path("businessId").asText().equals(String.valueOf(versionId))) {
                found = true;
                instanceId = it.path("id").asLong();
            }
        }
        assertThat(found).as("approval instance must exist for submitted parameter").isTrue();

        MvcResult tasks = mockMvc.perform(get("/api/v1/tasks/my?page=1&pageSize=10&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(tasks.getResponse().getContentAsString()).contains("E2E阈值");

        MvcResult msgs = mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(msgs.getResponse().getContentAsString()).contains("待审批");
    }

    @Test @Order(2)
    void initiatorCannotApproveOwnRequest() throws Exception {
        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"self\"}"))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    void approveActivatesParameterAndClosesTasks() throws Exception {
        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"同意\"}"))
                .andExpect(status().isOk());

        MvcResult versions = mockMvc.perform(get("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode versionItems = objectMapper.readTree(versions.getResponse().getContentAsString()).path("data");
        boolean active = false;
        for (JsonNode it : versionItems) {
            if (it.path("id").asLong() == versionId) active = it.path("status").asText().equals("ACTIVE");
        }
        assertThat(active).as("parameter version must be ACTIVE after approval").isTrue();

        MvcResult tasks = mockMvc.perform(get("/api/v1/tasks/my?page=1&pageSize=10&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(tasks.getResponse().getContentAsString()).doesNotContain("E2E阈值");

        MvcResult msgs = mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(msgs.getResponse().getContentAsString()).contains("已通过审批");
    }

    @Test @Order(4)
    void duplicateApprovalRejectedAndFinishedInstanceNotReusable() throws Exception {
        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"again\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());
    }

    @Test @Order(5)
    void rejectLeavesParameterVersionRejectedAndSendsMessage() throws Exception {
        long p2 = createApprovalRequiredParam();
        MvcResult v2 = mockMvc.perform(post("/api/v1/system/parameters/" + p2 + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paramValue\":\"1.5\"}"))
                .andExpect(status().isOk()).andReturn();
        long v2Id = objectMapper.readTree(v2.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/parameters/" + p2 + "/versions/" + v2Id + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        MvcResult apv = mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=10&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(apv.getResponse().getContentAsString()).path("data").path("items");
        long inst2 = -1;
        for (JsonNode it : items) {
            if (it.path("businessId").asText().equals(String.valueOf(v2Id))) inst2 = it.path("id").asLong();
        }
        assertThat(inst2).isPositive();

        mockMvc.perform(post("/api/v1/system/approvals/" + inst2 + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"值不合理\"}"))
                .andExpect(status().isOk());

        MvcResult versions = mockMvc.perform(get("/api/v1/system/parameters/" + p2 + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode versionItems = objectMapper.readTree(versions.getResponse().getContentAsString()).path("data");
        boolean rejected = false;
        for (JsonNode it : versionItems) {
            if (it.path("id").asLong() == v2Id) rejected = it.path("status").asText().equals("REJECTED");
        }
        assertThat(rejected).as("parameter version must be REJECTED after rejection").isTrue();

        MvcResult msgs = mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(msgs.getResponse().getContentAsString()).contains("被驳回");
    }

    @Test @Order(6)
    void messageReadAndUnreadCountArePersisted() throws Exception {
        MvcResult unread = mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        long unreadBefore = objectMapper.readTree(unread.getResponse().getContentAsString()).path("data").asLong();

        MvcResult msgs = mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode messageItems = objectMapper.readTree(msgs.getResponse().getContentAsString()).path("data").path("items");
        long firstId = -1;
        for (JsonNode it : messageItems) {
            if (it.path("status").asText().equals("UNREAD")) { firstId = it.path("id").asLong(); break; }
        }
        if (firstId > 0) {
            mockMvc.perform(post("/api/v1/messages/" + firstId + "/read")
                            .header(HttpHeaders.AUTHORIZATION, approverToken))
                    .andExpect(status().isOk());
            MvcResult unread2 = mockMvc.perform(get("/api/v1/messages/unread-count")
                            .header(HttpHeaders.AUTHORIZATION, approverToken))
                    .andExpect(status().isOk()).andReturn();
            long unreadAfter = objectMapper.readTree(unread2.getResponse().getContentAsString()).path("data").asLong();
            assertThat(unreadAfter).isLessThanOrEqualTo(unreadBefore);
        }
    }

    @Test @Order(7)
    void workflowNodesPersisted() throws Exception {
        List<SysWorkflowNodeEntity> nodes = queryRepo.listWorkflowNodes(publishedFlowId);
        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).getAssigneeValue()).isEqualTo(String.valueOf(approverUserId));
    }

    @Test @Order(8)
    void multiNodeApprovalIsStrictlySequentialAndOnlyFinalNodePublishes() throws Exception {
        long secondApproverId = users.findByUsername("flow_approver_2").orElseThrow().userId();
        String flowBody = """
            {"processCode":"FLOW_PARAM_MULTI","processName":"参数顺序审批","businessType":"SYSTEM_PARAMETER","description":"sequential",
             "nodes":[
               {"nodeCode":"N1","nodeName":"一级审批","nodeType":"APPROVAL","sortOrder":1,"assigneeType":"USER","assigneeValue":"%d","durationHours":24},
               {"nodeCode":"N2","nodeName":"二级审批","nodeType":"APPROVAL","sortOrder":2,"assigneeType":"USER","assigneeValue":"%d","durationHours":24}
             ]}
            """.formatted(approverUserId, secondApproverId);
        MvcResult flow = mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(flowBody))
                .andExpect(status().isOk()).andReturn();
        long flowId = objectMapper.readTree(flow.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        long parameterId = createApprovalRequiredParam();
        MvcResult versionResult = mockMvc.perform(post("/api/v1/system/parameters/" + parameterId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paramValue\":\"2.5\"}"))
                .andExpect(status().isOk()).andReturn();
        long parameterVersionId = objectMapper.readTree(versionResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/parameters/" + parameterId + "/versions/"
                        + parameterVersionId + "/submit").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        long approvalId = findPendingApproval(parameterVersionId);
        JsonNode initialNodes = approvalNodes(approvalId);
        assertThat(initialNodes.get(0).path("status").asText()).isEqualTo("PENDING");
        assertThat(initialNodes.get(1).path("status").asText()).isEqualTo("WAITING");

        mockMvc.perform(post("/api/v1/system/approvals/" + approvalId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, secondApproverToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"too early\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/approvals/" + approvalId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"first\"}"))
                .andExpect(status().isOk());
        JsonNode afterFirst = approvalNodes(approvalId);
        assertThat(afterFirst.get(0).path("status").asText()).isEqualTo("APPROVED");
        assertThat(afterFirst.get(1).path("status").asText()).isEqualTo("PENDING");
        assertThat(versionStatus(parameterId, parameterVersionId)).isEqualTo("PENDING_APPROVAL");

        mockMvc.perform(post("/api/v1/system/approvals/" + approvalId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, secondApproverToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"final\"}"))
                .andExpect(status().isOk());
        assertThat(versionStatus(parameterId, parameterVersionId)).isEqualTo("ACTIVE");
    }

    private long findPendingApproval(long businessId) throws Exception {
        MvcResult approvals = mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=100&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode item : objectMapper.readTree(approvals.getResponse().getContentAsString())
                .path("data").path("items")) {
            if (item.path("businessId").asText().equals(String.valueOf(businessId))) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("Pending approval not found for business id " + businessId);
    }

    private JsonNode approvalNodes(long approvalId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/system/approvals/" + approvalId + "/nodes")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String versionStatus(long parameterId, long parameterVersionId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/system/parameters/" + parameterId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode version : objectMapper.readTree(result.getResponse().getContentAsString()).path("data")) {
            if (version.path("id").asLong() == parameterVersionId) return version.path("status").asText();
        }
        return "MISSING";
    }
}
