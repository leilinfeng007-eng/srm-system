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
                "status=" + processed.status(), processed.lastError());
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
                "status=" + retried.status(), retried.lastError());
        return retried;
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
        if (!repository.resetOutboxForRetry(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "Outbox event is not retryable");
        }
        OutboxEvent event = repository.findOutbox(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        audit.record("OUTBOX_EVENT_RETRY", "OUTBOX_EVENT", String.valueOf(id), "SUCCESS",
                "status=FAILED", "status=" + event.status(), null);
        return event;
    }

    public PageResult<InboxEvent> listInbox(int page, int size, String status) {
        requireAll(false);
        return repository.listInbox(page, size, status);
    }

    public PageResult<OutboxEvent> listOutbox(int page, int size, String status) {
        requireAll(false);
        return repository.listOutbox(page, size, status);
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
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
