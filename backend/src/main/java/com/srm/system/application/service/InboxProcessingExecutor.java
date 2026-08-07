package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.platform.integration.InboxEventPayload;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.repository.IntegrationEventRepository;
import com.srm.system.domain.service.AuditRecorder;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxProcessingExecutor {

    private final IntegrationEventRepository repository;
    private final InboxHandlerRegistry handlerRegistry;
    private final AuditRecorder audit;
    private final Clock clock;

    public InboxProcessingExecutor(IntegrationEventRepository repository,
                                   InboxHandlerRegistry handlerRegistry,
                                   AuditRecorder audit, Clock clock) {
        this.repository = repository;
        this.handlerRegistry = handlerRegistry;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long eventId) {
        InboxEvent event = repository.findInbox(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!repository.claimInbox(eventId, LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CONFLICT, "Inbox event is not claimable");
        }
        audit.record("INBOX_EVENT_CLAIMED", "INBOX_EVENT", String.valueOf(eventId), "SUCCESS",
                "status=RECEIVED", "status=RECEIVED->PROCESSING,attempt=" + event.attemptCount()
                        + ",manual=false", null);
        long latestVersion = repository.findLatestProcessedVersion(
                event.sourceSystem(), event.objectType(), event.objectId());
        if (latestVersion >= event.objectVersion()) {
            repository.markInboxIgnoredStale(event.id(), LocalDateTime.now(clock),
                    "A newer or equal object version is already processed");
            audit.record("INBOX_EVENT_IGNORED_STALE", "INBOX_EVENT", String.valueOf(eventId),
                    "SUCCESS", "status=PROCESSING", "status=PROCESSING->IGNORED_STALE,attempt="
                            + event.attemptCount() + ",manual=false",
                    "A newer or equal object version is already processed");
            return;
        }
        InboxEventPayload payload = new InboxEventPayload(event.eventId(), event.sourceSystem(),
                event.objectType(), event.objectId(), event.objectVersion(), event.payload());
        handlerRegistry.require(event.objectType()).handle(payload);
        repository.markInboxProcessed(event.id(), LocalDateTime.now(clock));
        audit.record("INBOX_EVENT_PROCESSED", "INBOX_EVENT", String.valueOf(eventId), "SUCCESS",
                "status=PROCESSING", "status=PROCESSING->PROCESSED,attempt=" + event.attemptCount()
                        + ",manual=false", null);
    }
}
