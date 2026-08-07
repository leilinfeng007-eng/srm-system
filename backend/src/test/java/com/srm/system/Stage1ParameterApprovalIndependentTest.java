package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.UserAccountRepository;
import com.srm.system.infrastructure.persistence.MybatisParameterRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 参数版本审批闭环独立验收测试。
 * 每个测试方法独立创建参数、版本、流程提交与审批实例，不依赖执行顺序，
 * 不共享其他测试方法产生的业务数据。流程与审批人账户仅作为本类公共夹具。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Stage1ParameterApprovalIndependentTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MybatisParameterRepository paramRepo;

    private static String adminToken;
    private static String approverToken;
    private static Long approverId;

    @BeforeAll
    void setup() throws Exception {
        adminToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        approverId = users.createUserIfMissing("pai_approver",
                passwordEncoder.encode("PaiApprover!2026"), "PAI Approver", "test");
        users.assignRoleIfMissing(approverId, "PROCESS_ADMIN", "test");
        approverToken = login("pai_approver", "PaiApprover!2026");

        String flowBody = """
            {"processCode":"PAI_PARAM_FLOW","processName":"参数审批独立验收","businessType":"SYSTEM_PARAMETER",
             "description":"independent parameter approval",
             "nodes":[{"nodeCode":"N1","nodeName":"审批人","nodeType":"APPROVAL","sortOrder":1,
                       "assigneeType":"USER","assigneeValue":"%d","durationHours":48}]}
            """.formatted(approverId);
        MvcResult flow = mockMvc.perform(post("/api/v1/system/workflows")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(flowBody))
                .andExpect(status().isOk()).andReturn();
        long flowId = objectMapper.readTree(flow.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        String body = objectMapper.createObjectNode().put("username", username)
                .put("password", password).toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private long createApprovalRequiredParam(String code) throws Exception {
        String body = "{\"paramCode\":\"" + code + "\",\"paramName\":\"PAI 参数\",\"paramType\":\"STRING\","
                + "\"defaultValue\":\"default\",\"approvalRequired\":true}";
        MvcResult r = mockMvc.perform(post("/api/v1/system/parameters")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long createVersion(long paramId, String value) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paramValue\":\"" + value + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long submitVersion(long paramId, long versionId) throws Exception {
        mockMvc.perform(post("/api/v1/system/parameters/" + paramId + "/versions/" + versionId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
        MvcResult approvals = mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=100&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode item : objectMapper.readTree(approvals.getResponse().getContentAsString())
                .path("data").path("items")) {
            if (item.path("businessType").asText().equals("SYSTEM_PARAMETER")
                    && item.path("businessId").asText().equals(String.valueOf(versionId))) {
                return item.path("id").asLong();
            }
        }
        throw new AssertionError("Pending approval instance not found for version " + versionId);
    }

    private String versionStatus(long paramId, long versionId) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/system/parameters/" + paramId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode version : objectMapper.readTree(r.getResponse().getContentAsString()).path("data")) {
            if (version.path("id").asLong() == versionId) return version.path("status").asText();
        }
        return "MISSING";
    }

    @Test
    void parameterApprovalPassMakesNewVersionActiveAndEffective() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "PAI_PASS_" + suffix;
        long paramId = createApprovalRequiredParam(code);
        long versionId = createVersion(paramId, "v1-value");
        long instanceId = submitVersion(paramId, versionId);

        MvcResult tasks = mockMvc.perform(get("/api/v1/tasks/my?page=1&pageSize=20&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(tasks.getResponse().getContentAsString()).contains("PAI 参数");

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"同意\"}"))
                .andExpect(status().isOk());

        assertThat(versionStatus(paramId, versionId)).isEqualTo("ACTIVE");
        assertThat(paramRepo.getEffectiveValue(code)).isEqualTo("v1-value");
    }

    @Test
    void parameterRejectionKeepsPreviousActiveVersionEffective() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String code = "PAI_REJECT_" + suffix;
        long paramId = createApprovalRequiredParam(code);

        long v1Id = createVersion(paramId, "v1-effective");
        long v1Instance = submitVersion(paramId, v1Id);
        mockMvc.perform(post("/api/v1/system/approvals/" + v1Instance + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"同意\"}"))
                .andExpect(status().isOk());
        assertThat(versionStatus(paramId, v1Id)).isEqualTo("ACTIVE");
        assertThat(paramRepo.getEffectiveValue(code)).isEqualTo("v1-effective");

        long v2Id = createVersion(paramId, "v2-rejected");
        long v2Instance = submitVersion(paramId, v2Id);
        mockMvc.perform(post("/api/v1/system/approvals/" + v2Instance + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"值不合理\"}"))
                .andExpect(status().isOk());

        assertThat(versionStatus(paramId, v2Id)).isEqualTo("REJECTED");
        assertThat(versionStatus(paramId, v1Id)).isEqualTo("ACTIVE");
        assertThat(paramRepo.getEffectiveValue(code)).isEqualTo("v1-effective");
    }
}
