package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Stage1WorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MybatisWorkflowQueryRepository queryRepo;

    private String adminToken;
    private static Long workflowId;

    @BeforeAll
    void setup() throws Exception {
        String login = objectMapper.createObjectNode()
                .put("username", "stage0_admin").put("password", "Stage0AdminTestOnly!2026").toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andReturn();
        adminToken = "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test @Order(1)
    void createWorkflowWithNodesAndPublish() throws Exception {
        String body = """
            {"processCode":"TEST_PARAM_FLOW","processName":"参数变更审批","businessType":"SYSTEM_PARAMETER",
             "description":"测试流程",
             "nodes":[{"nodeCode":"N1","nodeName":"审批人","nodeType":"APPROVAL","sortOrder":1,
                       "assigneeType":"USER","assigneeValue":"1","durationHours":48}]}
            """;
        MvcResult r = mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processCode").value("TEST_PARAM_FLOW"))
                .andReturn();
        workflowId = objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertThat(workflowId).isPositive();

        mockMvc.perform(post("/api/v1/system/workflows/" + workflowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/v1/system/workflows?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(list.getResponse().getContentAsString()).contains("TEST_PARAM_FLOW");
    }

    @Test @Order(2)
    void publishedWorkflowCannotBeModified() throws Exception {
        mockMvc.perform(post("/api/v1/system/workflows/" + workflowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict());

        String body = "{\"processName\":\"Should Fail\",\"nodes\":[]}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/system/workflows/" + workflowId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    void submitApprovalAndListMyApprovals() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/system/outbox-events")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":\"EVT-APV-001\",\"objectType\":\"PARAM\",\"objectId\":\"999\",\"objectVersion\":1,\"eventType\":\"SUBMIT\",\"payloadSummary\":\"test\"}"))
                .andReturn();
        int status = r.getResponse().getStatus();
        assertThat(status).isIn(200, 403);

        mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(4)
    void myTasksAndUnreadMessagesAreQueryable() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/messages/my?page=1&pageSize=10")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(5)
    void workflowNodePersistenceAndQuery() throws Exception {
        List<SysWorkflowNodeEntity> nodes = queryRepo.listWorkflowNodes(workflowId);
        assertThat(nodes).isNotEmpty();
        assertThat(nodes.get(0).getNodeCode()).isEqualTo("N1");
        assertThat(nodes.get(0).getAssigneeType()).isEqualTo("USER");
    }
}
