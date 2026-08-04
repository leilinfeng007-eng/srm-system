package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.platform.integration.InboxEventPayload;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.repository.IntegrationEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxProcessingExecutor {

    private final IntegrationEventRepository repository;
    private final InboxHandlerRegistry handlerRegistry;
    private final Clock clock;

    public InboxProcessingExecutor(IntegrationEventRepository repository,
                                   InboxHandlerRegistry handlerRegistry, Clock clock) {
        this.repository = repository;
        this.handlerRegistry = handlerRegistry;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long eventId) {
        InboxEvent event = repository.findInbox(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!repository.claimInbox(eventId, LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.CONFLICT, "Inbox event is not claimable");
        }
        long latestVersion = repository.findLatestProcessedVersion(
                event.sourceSystem(), event.objectType(), event.objectId());
        if (latestVersion >= event.objectVersion()) {
            repository.markInboxIgnoredStale(event.id(), LocalDateTime.now(clock),
                    "A newer or equal object version is already processed");
            return;
        }
        InboxEventPayload payload = new InboxEventPayload(event.eventId(), event.sourceSystem(),
                event.objectType(), event.objectId(), event.objectVersion(), event.payload());
        handlerRegistry.require(event.objectType()).handle(payload);
        repository.markInboxProcessed(event.id(), LocalDateTime.now(clock));
    }
}
