package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.auth.UserAccountRepository;
import com.srm.system.application.service.ApprovalCallbackRegistry;
import com.srm.system.application.service.ApprovalCallbackRegistry.ApprovalCallback;
import com.srm.system.application.service.ApprovalService;
import com.srm.system.application.service.OverdueTaskScanner;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.system.infrastructure.persistence.entity.SysApprovalInstanceEntity;
import com.srm.system.infrastructure.persistence.entity.SysApprovalNodeInstanceEntity;
import com.srm.system.infrastructure.persistence.entity.SysMessageEntity;
import com.srm.system.infrastructure.persistence.entity.SysTaskEntity;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ContextConfiguration(classes = {Stage1SlaTest.TestClockConfig.class})
class Stage1SlaTest {

    static final Instant FIXED_INSTANT = Instant.parse("2026-08-03T00:00:00Z");
    static final MutableClock TEST_CLOCK = new MutableClock(FIXED_INSTANT);

    static final class MutableClock extends Clock {
        private volatile Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advanceSeconds(long s) { instant = instant.plusSeconds(s); }
        @Override public Instant instant() { return instant; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClockConfig {
        @Bean @Primary
        Clock fixedClock() {
            return TEST_CLOCK;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OverdueTaskScanner scanner;
    @Autowired private ApprovalService approvalService;
    @Autowired private SysApprovalNodeInstanceMapper nodeMapper;
    @Autowired private SysApprovalInstanceMapper instanceMapper;
    @Autowired private SysTaskMapper taskMapper;
    @Autowired private SysMessageMapper messageMapper;
    @Autowired private ApprovalCallbackRegistry callbackRegistry;

    private static String initiatorToken;
    private static String approverToken;
    private static String viewerToken;
    private static Long approverId;

    private String login(String u, String p) throws Exception {
        String body = objectMapper.createObjectNode().put("username", u).put("password", p).toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @BeforeAll
    void setup() throws Exception {
        callbackRegistry.register(new ApprovalCallback() {
            @Override public String businessType() { return "SLA_TEST"; }
            @Override public void onApproved(String businessId) { }
            @Override public void onRejected(String businessId) { }
        });
        initiatorToken = login("stage0_admin", "Stage0AdminTestOnly!2026");
        approverId = users.createUserIfMissing("sla_approver", passwordEncoder.encode("SlaApprover!2026"), "SLA Approver", "test");
        users.assignRoleIfMissing(approverId, "SYSTEM_ADMIN", "test");
        approverToken = login("sla_approver", "SlaApprover!2026");
        viewerToken = login("stage0_viewer", "Stage0ViewerTestOnly!2026");

        String flow = """
            {"processCode":"FLOW_SLA","processName":"SLA流程","businessType":"SLA_TEST","description":"x",
             "nodes":[{"nodeCode":"N1","nodeName":"审批人","nodeType":"APPROVAL","sortOrder":1,"assigneeType":"USER","assigneeValue":"%d","durationHours":1}]}
            """.formatted(approverId);
        MvcResult f = mockMvc.perform(post("/api/v1/system/workflows").header(HttpHeaders.AUTHORIZATION, initiatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(flow))
                .andExpect(status().isOk()).andReturn();
        long flowId = objectMapper.readTree(f.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish").header(HttpHeaders.AUTHORIZATION, initiatorToken))
                .andExpect(status().isOk());
    }

    private long submitSlaInstance() throws Exception {
        var principal = users.findByUsername("stage0_admin").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Object submitted = approvalService.submit("SLA_TEST", "OBJ-" + System.nanoTime(), "SLA实例");
        assertThat(submitted).isNotNull();
        var insts = instanceMapper.selectList(new LambdaQueryWrapper<SysApprovalInstanceEntity>()
                .eq(SysApprovalInstanceEntity::getBusinessType, "SLA_TEST")
                .eq(SysApprovalInstanceEntity::getStatus, "PENDING")
                .orderByDesc(SysApprovalInstanceEntity::getId)
                .last("LIMIT 1"));
        assertThat(insts).isNotEmpty();
        SecurityContextHolder.clearContext();
        return insts.get(0).getId();
    }

    @Test @Order(1)
    void withdrawSuccessClosesTasksAndNotifiesApprover() throws Exception {
        long instanceId = submitSlaInstance();

        MvcResult w = mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, initiatorToken))
                .andExpect(status().isOk()).andReturn();
        assertThat(w.getResponse().getStatus()).isEqualTo(200);

        SysApprovalInstanceEntity instance = instanceMapper.selectById(instanceId);
        assertThat(instance.getStatus()).isEqualTo("WITHDRAWN");

        var instanceNodes = nodeMapper.selectList(new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId));
        long openTasks = 0;
        for (var n : instanceNodes) {
            openTasks += taskMapper.selectCount(new LambdaQueryWrapper<SysTaskEntity>()
                    .eq(SysTaskEntity::getSourceType, "APPROVAL")
                    .eq(SysTaskEntity::getSourceId, String.valueOf(n.getId()))
                    .eq(SysTaskEntity::getStatus, "PENDING"));
        }
        assertThat(openTasks).as("no PENDING approval tasks may remain after withdraw").isZero();

        long nodeCancelled = nodeMapper.selectCount(new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)
                .eq(com.srm.system.infrastructure.persistence.entity.SysApprovalNodeInstanceEntity::getStatus, "CANCELLED"));
        assertThat(nodeCancelled).isEqualTo(1);

        long withdrawMsgs = messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getSourceType, "APPROVAL_WITHDRAWN")
                .eq(SysMessageEntity::getSourceId, String.valueOf(instanceId)));
        assertThat(withdrawMsgs).isEqualTo(1);
    }

    @Test @Order(2)
    void withdrawRestrictionsEnforced() throws Exception {
        long instanceId = submitSlaInstance();

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, approverToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, viewerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"x\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, initiatorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, initiatorToken))
                .andExpect(status().isConflict());
    }

    @Test @Order(3)
    void nonCurrentApproverCannotApprove() throws Exception {
        long instanceId = submitSlaInstance();
        mockMvc.perform(post("/api/v1/system/approvals/" + instanceId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, initiatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"self\"}"))
                .andExpect(status().isConflict());
    }

    @Test @Order(4)
    void slaOverdueScanMarksOnceAndSendsSingleMessage() throws Exception {
        long instanceId = submitSlaInstance();
        SysApprovalInstanceEntity instance = instanceMapper.selectById(instanceId);
        var node = nodeMapper.selectList(new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)).get(0);

        TEST_CLOCK.advanceSeconds(7200);
        int first = scanner.scanOnce();
        assertThat(first).isPositive();
        assertThat(nodeMapper.selectById(node.getId()).getOverdueNotified()).isTrue();

        int second = scanner.scanOnce();
        assertThat(second).isZero();

        long msgs = messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getSourceType, "APPROVAL_OVERDUE")
                .eq(com.srm.system.infrastructure.persistence.entity.SysMessageEntity::getSourceId, String.valueOf(node.getId())));
        assertThat(msgs).isEqualTo(1);
    }

    @Test @Order(5)
    void callbackFailureRollsBackApprovalState() throws Exception {
        callbackRegistry.register(new ApprovalCallback() {
            @Override public String businessType() { return "SLA_FAIL"; }
            @Override public void onApproved(String businessId) { throw new IllegalStateException("boom"); }
            @Override public void onRejected(String businessId) { throw new IllegalStateException("boom"); }
        });

        String flow = """
            {"processCode":"FLOW_SLA_FAIL","processName":"失败流程","businessType":"SLA_FAIL","description":"x",
             "nodes":[{"nodeCode":"N1","nodeName":"审批人","nodeType":"APPROVAL","sortOrder":1,"assigneeType":"USER","assigneeValue":"%d","durationHours":1}]}
            """.formatted(approverId);
        MvcResult f = mockMvc.perform(post("/api/v1/system/workflows").header(HttpHeaders.AUTHORIZATION, initiatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(flow))
                .andExpect(status().isOk()).andReturn();
        long flowId = objectMapper.readTree(f.getResponse().getContentAsString()).path("data").path("id").asLong();
        mockMvc.perform(post("/api/v1/system/workflows/" + flowId + "/publish").header(HttpHeaders.AUTHORIZATION, initiatorToken))
                .andExpect(status().isOk());

        var principal = users.findByUsername("stage0_admin").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        approvalService.submit("SLA_FAIL", "FAIL-OBJ-1", "失败实例");
        SecurityContextHolder.clearContext();

        MvcResult apv = mockMvc.perform(get("/api/v1/system/approvals?page=1&pageSize=10&status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, initiatorToken)).andExpect(status().isOk()).andReturn();
        JsonNode items = objectMapper.readTree(apv.getResponse().getContentAsString()).path("data").path("items");
        long failId = -1;
        for (JsonNode it : items) { if (it.path("businessType").asText().equals("SLA_FAIL")) failId = it.path("id").asLong(); }
        assertThat(failId).isPositive();

        MvcResult approveRes = mockMvc.perform(post("/api/v1/system/approvals/" + failId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, approverToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"x\"}"))
                .andReturn();
        assertThat(approveRes.getResponse().getStatus()).isEqualTo(500);

        SysApprovalInstanceEntity instance = instanceMapper.selectById(failId);
        assertThat(instance.getStatus()).as("instance must roll back to PENDING").isEqualTo("PENDING");

        var node = nodeMapper.selectList(new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getInstanceId, failId)).get(0);
        assertThat(node.getStatus()).isEqualTo("PENDING");

        long pendingAfter = taskMapper.selectCount(new LambdaQueryWrapper<SysTaskEntity>()
                .eq(SysTaskEntity::getStatus, "PENDING"));
        assertThat(pendingAfter).isPositive();

        long approvedMsgs = messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getSourceType, "APPROVAL_APPROVED")
                .eq(SysMessageEntity::getSourceId, String.valueOf(failId)));
        assertThat(approvedMsgs).as("no approve message may exist after rollback").isZero();
    }
}
