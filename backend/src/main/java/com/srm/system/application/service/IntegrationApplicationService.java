package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.platform.integration.InboxEventPayload;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.model.OutboxEvent;
import com.srm.system.domain.repository.IntegrationEventRepository;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.service.AuditRecorder;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class IntegrationApplicationService {

    private final IntegrationEventRepository repository;
    private final InboxProcessingExecutor inboxExecutor;
    private final Clock clock;
    private final DataScopeAuthorizationService scopes;
    private final AuditRecorder audit;

    public IntegrationApplicationService(IntegrationEventRepository repository,
                                         InboxProcessingExecutor inboxExecutor, Clock clock,
                                         DataScopeAuthorizationService scopes, AuditRecorder audit) {
        this.repository = repository;
        this.inboxExecutor = inboxExecutor;
        this.clock = clock;
        this.scopes = scopes;
        this.audit = audit;
    }

    public InboxEvent receiveInbox(InboxEventRequest request) {
        requireAll(true);
        var receipt = repository.receiveInbox(new InboxEventPayload(request.eventId(),
                request.sourceSystem(), request.objectType(), request.objectId(),
                request.objectVersion(), request.payload()));
        if (receipt.duplicate()) {
            audit.record("INBOX_DUPLICATE_RECEIVED", "INBOX_EVENT", String.valueOf(receipt.event().id()),
                    "SUCCESS", null, "eventId=" + receipt.event().eventId(), null);
            return receipt.event();
        }
        InboxEvent processed = processInbox(receipt.event().id());
        audit.record("INBOX_EVENT_RECEIVED", "INBOX_EVENT", String.valueOf(processed.id()),
                "FAILED".equals(processed.status()) ? "FAILED" : "SUCCESS", "status=RECEIVED",
                processingSummary(processed, "RECEIVED", false), processed.lastError());
        return processed;
    }

    public InboxEvent retryInbox(Long id) {
        requireAll(true);
        InboxEvent event = repository.findInbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!"FAILED".equals(event.status()) || event.attemptCount() >= event.maxAttempts()) {
            throw new BusinessException(ErrorCode.CONFLICT, "Inbox event is not retryable");
        }
        if (!repository.resetInboxForRetry(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Inbox event retry state changed");
        }
        InboxEvent retried = processInbox(id);
        audit.record("INBOX_EVENT_RETRY", "INBOX_EVENT", String.valueOf(id),
                "FAILED".equals(retried.status()) ? "FAILED" : "SUCCESS", "status=FAILED",
                processingSummary(retried, "FAILED", true), retried.lastError());
        return retried;
    }

    private String processingSummary(InboxEvent event, String beforeStatus, boolean manual) {
        return "status=" + beforeStatus + "->" + event.status()
                + ",attempt=" + event.attemptCount()
                + ",manual=" + manual
                + (event.nextRetryAt() == null ? "" : ",nextRetryAt=" + event.nextRetryAt());
    }

    private InboxEvent processInbox(Long id) {
        try {
            inboxExecutor.process(id);
        } catch (RuntimeException failure) {
            InboxEvent event = repository.findInbox(id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            int attempt = event.attemptCount() == null ? 1 : event.attemptCount() + 1;
            long delayMinutes = Math.min(60L, 1L << Math.min(attempt, 6));
            repository.markInboxFailed(id, safeMessage(failure),
                    LocalDateTime.now(clock).plusMinutes(delayMinutes));
        }
        return repository.findInbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public OutboxEvent createOutbox(OutboxEventRequest request) {
        requireAll(true);
        LocalDateTime now = LocalDateTime.now(clock);
        OutboxEvent event = repository.createOutbox(new OutboxEvent(null, request.eventId(),
                request.objectType(), request.objectId(), request.objectVersion(),
                request.eventType(), request.payloadSummary(), "READY", 0, 5,
                null, null, request.traceId(), now, now));
        audit.record("OUTBOX_EVENT_CREATED", "OUTBOX_EVENT", String.valueOf(event.id()),
                "SUCCESS", null, "eventId=" + event.eventId() + ",status=" + event.status(), null);
        return event;
    }

    public OutboxEvent retryOutbox(Long id) {
        requireAll(true);
        OutboxEvent before = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!repository.resetOutboxForRetry(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Outbox event is not retryable");
        }
        OutboxEvent event = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        audit.record("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", String.valueOf(id), "SUCCESS",
                "status=" + before.status(),
                "status=" + before.status() + "->" + event.status()
                        + ",attempt=" + event.attemptCount() + ",manual=true", null);
        return event;
    }

    /**
     * Real processing primitive: atomically claims a READY outbox event.
     * A FAILED event that is still backing off (next_retry_at in the future)
     * is not claimable; only the system release step ({@link #releaseDueOutbox})
     * re-queues it after its retry time. Never touches DEAD events.
     */
    public OutboxEvent claimOutbox(Long id) {
        requireAll(true);
        OutboxEvent before = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!repository.claimOutbox(id, LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Outbox event is not claimable or is still backing off");
        }
        OutboxEvent claimed = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        audit.record("OUTBOX_EVENT_CLAIMED", "OUTBOX_EVENT", String.valueOf(id), "SUCCESS",
                "status=" + before.status(),
                "status=" + before.status() + "->" + claimed.status()
                        + ",attempt=" + claimed.attemptCount() + ",manual=false", null);
        return claimed;
    }

    /**
     * Real processing primitive: marks a claimed (PROCESSING) outbox event as
     * published after an actual external delivery succeeded. This is the only
     * place that records a successful delivery; management commands such as
     * create/retry are never reported as processing success.
     */
    public OutboxEvent publishOutbox(Long id) {
        requireAll(true);
        if (!repository.markOutboxPublished(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Outbox event is not in PROCESSING state");
        }
        OutboxEvent published = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        audit.record("OUTBOX_EVENT_PUBLISHED", "OUTBOX_EVENT", String.valueOf(id), "SUCCESS",
                "status=PROCESSING",
                "status=PROCESSING->" + published.status() + ",attempt=" + published.attemptCount()
                        + ",manual=false", null);
        return published;
    }

    /**
     * Real processing primitive: records a failed delivery attempt for a
     * claimed event, increments the attempt counter, computes the next retry
     * time with exponential backoff, and moves the event to DEAD once
     * max_attempts is reached. DEAD events are never re-claimed.
     */
    public OutboxEvent failOutbox(Long id, String error, Duration backoff) {
        requireAll(true);
        repository.markOutboxFailed(id, safeMessage(error),
                LocalDateTime.now(clock).plus(backoff));
        OutboxEvent failed = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        boolean dead = "DEAD".equals(failed.status());
        audit.record(dead ? "OUTBOX_EVENT_DEAD" : "OUTBOX_EVENT_FAILED", "OUTBOX_EVENT",
                String.valueOf(id), "FAILED", "status=PROCESSING",
                "status=PROCESSING->" + failed.status() + ",attempt=" + failed.attemptCount()
                        + ",nextRetryAt=" + failed.nextRetryAt() + ",manual=false",
                safeMessage(error));
        return failed;
    }

    /**
     * System retry step: atomically re-queues the FAILED event whose retry
     * time has arrived (respecting attempt_count and next_retry_at). Returns
     * null when no event is due. This is the only path a real outbox
     * publisher may use to obtain a due event.
     */
    public Long releaseDueOutbox() {
        requireAll(true);
        return repository.releaseDueOutbox(LocalDateTime.now(clock));
    }

    public PageResult<InboxEvent> listInbox(int page, int size, String status, String sourceSystem,
                                            String objectType, LocalDateTime from, LocalDateTime to) {
        requireAll(false);
        return repository.listInbox(page, size, status, sourceSystem, objectType, from, to);
    }

    public PageResult<OutboxEvent> listOutbox(int page, int size, String status, String eventType,
                                              String objectType, LocalDateTime from, LocalDateTime to) {
        requireAll(false);
        return repository.listOutbox(page, size, status, eventType, objectType, from, to);
    }

    public InboxEvent getInbox(Long id) {
        requireAll(false);
        return repository.findInbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public OutboxEvent getOutbox(Long id) {
        requireAll(false);
        return repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requireAll(boolean write) {
        var scope = write
                ? scopes.requireWrite("system:integration-job:retry", "system", "ORGANIZATION")
                : scopes.requireRead("system:integration-job:view", "system", "ORGANIZATION");
        if (!scope.isAllScope()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) return failure.getClass().getSimpleName();
        return safeMessage(message);
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) return "unknown error";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
