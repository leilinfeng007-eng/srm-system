package com.srm.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.common.exception.BusinessException;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.application.service.IntegrationApplicationService;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.model.OutboxEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Inbox/Outbox 真实处理记录与失败退避测试。
 * 使用可控时钟推进时间，不依赖真实等待，不依赖测试执行顺序；
 * 每个测试独立构造事件（唯一 eventId），不共享残留数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = {Stage1InboxOutboxProcessingTest.TestClockConfig.class})
class Stage1InboxOutboxProcessingTest {

    static final Instant FIXED_INSTANT = Instant.parse("2026-08-01T00:00:00Z");
    static final MutableClock TEST_CLOCK = new MutableClock(FIXED_INSTANT);

    static final class MutableClock extends Clock {
        private volatile Instant instant;
        MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
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
    @Autowired private IntegrationApplicationService service;

    @BeforeEach
    void setupAuth() {
        asAdmin();
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void asAdmin() {
        var principal = new SrmPrincipal(1L, "stage0_admin", "", "Stage0 Admin", "ACTIVE",
                false, List.of("SUPER_ADMIN"), List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private String login() throws Exception {
        String body = objectMapper.createObjectNode()
                .put("username", "stage0_admin").put("password", "Stage0AdminTestOnly!2026").toString();
        MvcResult r = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private JsonNode attempts(String targetType, Long id) throws Exception {
        String token = login();
        String path = "/api/v1/system/" + ("INBOX_EVENT".equals(targetType) ? "inbox-events" : "outbox-events")
                + "/" + id + "/attempts";
        MvcResult r = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).path("data");
    }

    private String adminToken() throws Exception {
        return login();
    }

    // ============================================================
    // Inbox 真实处理记录
    // ============================================================

    @Test
    void inboxFirstProcessingSuccessIsRecorded() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        InboxEvent event = service.receiveInbox(new InboxEventRequest("IOP-INBOX-OK-" + suffix,
                "ERP", "MATERIAL", "IOP-M-" + suffix, 1L,
                "{\"materialCode\":\"IOP-M-" + suffix + "\",\"materialName\":\"IOP Ok\",\"materialType\":\"RAW\",\"baseUnit\":\"EA\"}"));
        assertThat(event.status()).isEqualTo("PROCESSED");

        JsonNode attempts = attempts("INBOX_EVENT", event.id());
        assertThat(attempts.toString())
                .contains("INBOX_EVENT_RECEIVED")
                .contains("INBOX_EVENT_CLAIMED")
                .contains("INBOX_EVENT_PROCESSED")
                .contains("manual=false");
    }

    @Test
    void inboxFirstProcessingFailureIsRecordedWithReason() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        InboxEvent event = service.receiveInbox(new InboxEventRequest("IOP-INBOX-FAIL-" + suffix,
                "ERP", "NO_SUCH_HANDLER", "X-" + suffix, 1L, "{}"));
        assertThat(event.status()).isEqualTo("FAILED");

        JsonNode attempts = attempts("INBOX_EVENT", event.id());
        String body = attempts.toString();
        assertThat(body).contains("INBOX_EVENT_RECEIVED");
        assertThat(body).contains("FAILED");
        assertThat(body).contains("No Inbox handler registered");
    }

    @Test
    void inboxManualRetryIsRecorded() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        InboxEvent event = service.receiveInbox(new InboxEventRequest("IOP-INBOX-RETRY-" + suffix,
                "ERP", "NO_SUCH_HANDLER", "Y-" + suffix, 1L, "{}"));
        assertThat(event.status()).isEqualTo("FAILED");

        String token = adminToken();
        mockMvc.perform(post("/api/v1/system/inbox-events/" + event.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        JsonNode attempts = attempts("INBOX_EVENT", event.id());
        assertThat(attempts.toString())
                .contains("INBOX_EVENT_RETRY")
                .contains("manual=true");
    }

    // ============================================================
    // Outbox 领取、发布、失败退避、终态与并发
    // ============================================================

    @Test
    void outboxClaimAndPublishSuccessAreRecorded() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        OutboxEvent created = service.createOutbox(new OutboxEventRequest("IOP-OBX-OK-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "trace-" + suffix));
        assertThat(created.status()).isEqualTo("READY");

        OutboxEvent claimed = service.claimOutbox(created.id());
        assertThat(claimed.status()).isEqualTo("PROCESSING");

        OutboxEvent published = service.publishOutbox(created.id());
        assertThat(published.status()).isEqualTo("PUBLISHED");

        JsonNode attempts = attempts("OUTBOX_EVENT", created.id());
        String body = attempts.toString();
        assertThat(body)
                .contains("OUTBOX_EVENT_CREATED")
                .contains("OUTBOX_EVENT_CLAIMED")
                .contains("OUTBOX_EVENT_PUBLISHED");
        assertThat(body).doesNotContain("OUTBOX_EVENT_RETRY");
    }

    @Test
    void outboxFailureBacksOffAndIsNotClaimableBeforeRetryTime() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        OutboxEvent created = service.createOutbox(new OutboxEventRequest("IOP-OBX-BACKOFF-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "trace-" + suffix));
        service.claimOutbox(created.id());
        OutboxEvent failed = service.failOutbox(created.id(), "network timeout", Duration.ofMinutes(10));
        assertThat(failed.status()).isEqualTo("FAILED");
        assertThat(failed.attemptCount()).isEqualTo(1);
        assertThat(failed.nextRetryAt()).isNotNull();
        assertThat(failed.nextRetryAt()).isEqualTo(
                LocalDateTime.now(TEST_CLOCK).plusMinutes(10));

        assertThatThrownBy(() -> service.claimOutbox(created.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not claimable");

        asAdmin();
        JsonNode attempts = attempts("OUTBOX_EVENT", created.id());
        String body = attempts.toString();
        assertThat(body).contains("OUTBOX_EVENT_FAILED").contains("nextRetryAt=");
        assertThat(body).contains("network timeout");
    }

    @Test
    void outboxDueEventIsRequeuedAfterRetryTimeAndClaimable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        OutboxEvent created = service.createOutbox(new OutboxEventRequest("IOP-OBX-DUE-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "trace-" + suffix));
        service.claimOutbox(created.id());
        service.failOutbox(created.id(), "timeout", Duration.ofMinutes(10));

        TEST_CLOCK.advance(Duration.ofMinutes(9));
        Long releasedEarly = service.releaseDueOutbox();
        assertThat(releasedEarly).isNull();

        TEST_CLOCK.advance(Duration.ofMinutes(2));
        Long released = dueOutboxOf(created.id());
        assertThat(released).isEqualTo(created.id());

        asAdmin();
        OutboxEvent claimed = service.claimOutbox(created.id());
        assertThat(claimed.status()).isEqualTo("PROCESSING");
    }

    @Test
    void outboxReachesDeadAfterMaxAttemptsAndIsNeverReclaimable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        OutboxEvent created = service.createOutbox(new OutboxEventRequest("IOP-OBX-DEAD-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "trace-" + suffix));
        OutboxEvent current = created;
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (attempt > 1) service.retryOutbox(created.id());
            service.claimOutbox(created.id());
            current = service.failOutbox(created.id(), "attempt " + attempt, Duration.ofMinutes(10));
        }
        assertThat(current.status()).isEqualTo("DEAD");
        assertThat(current.attemptCount()).isEqualTo(5);

        assertThatThrownBy(() -> service.claimOutbox(created.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not claimable");

        TEST_CLOCK.advance(Duration.ofHours(1));
        while (service.releaseDueOutbox() != null) { /* drain residual due events from other tests */ }
        assertThat(service.getOutbox(created.id()).status()).isEqualTo("DEAD");
        assertThat(service.getOutbox(created.id()).attemptCount()).isEqualTo(5);

        asAdmin();
        JsonNode attempts = attempts("OUTBOX_EVENT", created.id());
        String body = attempts.toString();
        assertThat(body)
                .contains("OUTBOX_EVENT_FAILED")
                .contains("OUTBOX_EVENT_DEAD")
                .contains("status=PROCESSING->DEAD");
    }

    @Test
    void terminalOrExhaustedEventsRejectManualRetryWith409() throws Exception {
        String suffix = String.valueOf(System.nanoTime());

        OutboxEvent published = service.createOutbox(new OutboxEventRequest("IOP-OBX-409-PUB-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "t"));
        service.claimOutbox(published.id());
        service.publishOutbox(published.id());

        OutboxEvent processing = service.createOutbox(new OutboxEventRequest("IOP-OBX-409-PRO-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "t"));
        service.claimOutbox(processing.id());

        OutboxEvent dead = service.createOutbox(new OutboxEventRequest("IOP-OBX-409-DEAD-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "t"));
        for (int attempt = 1; attempt <= 5; attempt++) {
            if (attempt > 1) service.retryOutbox(dead.id());
            service.claimOutbox(dead.id());
            service.failOutbox(dead.id(), "x", Duration.ofMinutes(1));
        }
        assertThat(service.getOutbox(dead.id()).status()).isEqualTo("DEAD");

        InboxEvent inboxDead = service.receiveInbox(new InboxEventRequest("IOP-INBOX-409-" + suffix,
                "ERP", "NO_SUCH_HANDLER", "Z-" + suffix, 1L, "{}"));
        assertThat(inboxDead.status()).isEqualTo("FAILED");

        String token = adminToken();
        mockMvc.perform(post("/api/v1/system/outbox-events/" + published.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/system/outbox-events/" + processing.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/system/outbox-events/" + dead.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());

        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post("/api/v1/system/inbox-events/" + inboxDead.id() + "/retry")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/system/inbox-events/" + inboxDead.id())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.data.status").value("DEAD"));
        mockMvc.perform(post("/api/v1/system/inbox-events/" + inboxDead.id() + "/retry")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentClaimersOnlyOneSucceeds() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        OutboxEvent created = service.createOutbox(new OutboxEventRequest("IOP-OBX-CONC-" + suffix,
                "MATERIAL", "M-" + suffix, 1L, "CREATED", "iop", "t"));

        int workerCount = 2;
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(workerCount);
        AtomicInteger successes = new AtomicInteger();
        try {
            List<java.util.concurrent.Future<Void>> futures = java.util.stream.IntStream.range(0, workerCount)
                    .mapToObj(i -> executor.submit((Callable<Void>) () -> {
                        start.await();
                        asAdmin();
                        try {
                            service.claimOutbox(created.id());
                            successes.incrementAndGet();
                        } catch (BusinessException ignored) {
                        }
                        return null;
                    })).toList();
            start.countDown();
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(successes.get()).isEqualTo(1);
    }

    // ============================================================
    // helpers
    // ============================================================

    private Long dueOutboxOf(Long expectedId) {
        for (int i = 0; i < 50; i++) {
            Long due = service.releaseDueOutbox();
            if (due == null) return null;
            if (due.equals(expectedId)) return due;
        }
        return null;
    }
}
