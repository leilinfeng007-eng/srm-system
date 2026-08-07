package com.srm.system.domain.repository;

import com.srm.common.api.PageResult;
import com.srm.platform.integration.InboxEventPayload;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.model.OutboxEvent;
import java.time.LocalDateTime;
import java.util.Optional;

public interface IntegrationEventRepository {

    record InboxReceipt(InboxEvent event, boolean duplicate) {}

    InboxReceipt receiveInbox(InboxEventPayload payload);

    Optional<InboxEvent> findInbox(Long id);

    long findLatestProcessedVersion(String sourceSystem, String objectType, String objectId);

    boolean claimInbox(Long id, LocalDateTime startedAt);

    void markInboxProcessed(Long id, LocalDateTime processedAt);

    void markInboxIgnoredStale(Long id, LocalDateTime processedAt, String reason);

    void markInboxFailed(Long id, String error, LocalDateTime nextRetryAt);

    boolean resetInboxForRetry(Long id);

    default PageResult<InboxEvent> listInbox(int page, int size, String status) {
        return listInbox(page, size, status, null, null, null, null);
    }

    PageResult<InboxEvent> listInbox(int page, int size, String status, String sourceSystem,
                                     String objectType, LocalDateTime from, LocalDateTime to);

    OutboxEvent createOutbox(OutboxEvent event);

    Optional<OutboxEvent> findOutbox(Long id);

    boolean claimOutbox(Long id, LocalDateTime now);

    boolean markOutboxPublished(Long id);
    void markOutboxFailed(Long id, String error, LocalDateTime nextRetryAt);

    void markOutboxNoTransport(Long id, String reason);

    boolean resetOutboxForRetry(Long id);

    /**
     * Atomically moves one due FAILED outbox event back to READY.
     * A FAILED event is due when its next_retry_at has passed and it has
     * not exhausted max_attempts. Returns the event id, or null when no
     * event is due. Never touches DEAD or still-backing-off events.
     */
    Long releaseDueOutbox(LocalDateTime now);

    default PageResult<OutboxEvent> listOutbox(int page, int size, String status) {
        return listOutbox(page, size, status, null, null, null, null);
    }

    PageResult<OutboxEvent> listOutbox(int page, int size, String status, String eventType,
                                       String objectType, LocalDateTime from, LocalDateTime to);
}
