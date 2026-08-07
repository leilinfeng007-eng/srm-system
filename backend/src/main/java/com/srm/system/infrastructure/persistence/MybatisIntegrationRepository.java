package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.srm.common.api.PageResult;
import com.srm.platform.integration.InboxEventPayload;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.model.OutboxEvent;
import com.srm.system.domain.repository.IntegrationEventRepository;
import com.srm.system.infrastructure.persistence.entity.SysInboxEventEntity;
import com.srm.system.infrastructure.persistence.entity.SysOutboxEventEntity;
import com.srm.system.infrastructure.persistence.mapper.SysInboxEventMapper;
import com.srm.system.infrastructure.persistence.mapper.SysOutboxEventMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisIntegrationRepository implements IntegrationEventRepository {

    private final SysOutboxEventMapper outboxMapper;
    private final SysInboxEventMapper inboxMapper;

    public MybatisIntegrationRepository(SysOutboxEventMapper outboxMapper,
                                        SysInboxEventMapper inboxMapper) {
        this.outboxMapper = outboxMapper;
        this.inboxMapper = inboxMapper;
    }

    @Override
    @Transactional
    public InboxReceipt receiveInbox(InboxEventPayload payload) {
        SysInboxEventEntity existing = findInboxEntityByEventId(payload.eventId());
        if (existing != null) return new InboxReceipt(toDomain(existing), true);

        SysInboxEventEntity entity = new SysInboxEventEntity();
        entity.setEventId(payload.eventId());
        entity.setSourceSystem(payload.sourceSystem());
        entity.setObjectType(payload.objectType());
        entity.setObjectId(payload.objectId());
        entity.setObjectVersion(payload.objectVersion());
        entity.setPayloadSummary(payload.payload());
        entity.setStatus("RECEIVED");
        entity.setAttemptCount(0);
        entity.setMaxAttempts(5);
        entity.setReceivedAt(LocalDateTime.now());
        try {
            inboxMapper.insert(entity);
            return new InboxReceipt(toDomain(entity), false);
        } catch (DuplicateKeyException concurrentDuplicate) {
            SysInboxEventEntity concurrent = findInboxEntityByEventId(payload.eventId());
            if (concurrent == null) throw concurrentDuplicate;
            return new InboxReceipt(toDomain(concurrent), true);
        }
    }

    @Override
    public Optional<InboxEvent> findInbox(Long id) {
        return Optional.ofNullable(inboxMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public long findLatestProcessedVersion(String sourceSystem, String objectType, String objectId) {
        SysInboxEventEntity latest = inboxMapper.selectOne(new LambdaQueryWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getSourceSystem, sourceSystem)
                .eq(SysInboxEventEntity::getObjectType, objectType)
                .eq(SysInboxEventEntity::getObjectId, objectId)
                .eq(SysInboxEventEntity::getStatus, "PROCESSED")
                .orderByDesc(SysInboxEventEntity::getObjectVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getObjectVersion() == null ? -1L : latest.getObjectVersion();
    }

    @Override
    @Transactional
    public boolean claimInbox(Long id, LocalDateTime startedAt) {
        return inboxMapper.update(null, new LambdaUpdateWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getId, id)
                .eq(SysInboxEventEntity::getStatus, "RECEIVED")
                .set(SysInboxEventEntity::getStatus, "PROCESSING")
                .set(SysInboxEventEntity::getProcessingStartedAt, startedAt)) == 1;
    }

    @Override
    @Transactional
    public void markInboxProcessed(Long id, LocalDateTime processedAt) {
        requireInboxTransition(id, "PROCESSING", "PROCESSED", processedAt, "OK", null, null);
    }

    @Override
    @Transactional
    public void markInboxIgnoredStale(Long id, LocalDateTime processedAt, String reason) {
        requireInboxTransition(id, "PROCESSING", "IGNORED_STALE", processedAt, reason, null, null);
    }

    @Override
    @Transactional
    public void markInboxFailed(Long id, String error, LocalDateTime nextRetryAt) {
        SysInboxEventEntity entity = inboxMapper.selectById(id);
        if (entity == null) throw new IllegalStateException("Inbox event not found: " + id);
        int attempts = entity.getAttemptCount() == null ? 1 : entity.getAttemptCount() + 1;
        int maxAttempts = entity.getMaxAttempts() == null ? 5 : entity.getMaxAttempts();
        LambdaUpdateWrapper<SysInboxEventEntity> update = new LambdaUpdateWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getId, id)
                .in(SysInboxEventEntity::getStatus, "RECEIVED", "PROCESSING")
                .set(SysInboxEventEntity::getStatus, attempts >= maxAttempts ? "DEAD" : "FAILED")
                .set(SysInboxEventEntity::getAttemptCount, attempts)
                .set(SysInboxEventEntity::getLastError, error)
                .set(SysInboxEventEntity::getResultMessage, error)
                .set(SysInboxEventEntity::getProcessingStartedAt, null)
                .set(SysInboxEventEntity::getNextRetryAt, attempts >= maxAttempts ? null : nextRetryAt);
        if (inboxMapper.update(null, update) != 1) {
            throw new IllegalStateException("Inbox failure transition rejected: " + id);
        }
    }

    @Override
    @Transactional
    public boolean resetInboxForRetry(Long id) {
        return inboxMapper.update(null, new LambdaUpdateWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getId, id)
                .eq(SysInboxEventEntity::getStatus, "FAILED")
                .apply("attempt_count < max_attempts")
                .set(SysInboxEventEntity::getStatus, "RECEIVED")
                .set(SysInboxEventEntity::getNextRetryAt, null)
                .set(SysInboxEventEntity::getProcessingStartedAt, null)) == 1;
    }

    @Override
    public PageResult<InboxEvent> listInbox(int page, int size, String status, String sourceSystem,
                                            String objectType, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<SysInboxEventEntity> countQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysInboxEventEntity> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            countQuery.eq(SysInboxEventEntity::getStatus, status);
            query.eq(SysInboxEventEntity::getStatus, status);
        }
        if (sourceSystem != null && !sourceSystem.isBlank()) {
            countQuery.eq(SysInboxEventEntity::getSourceSystem, sourceSystem);
            query.eq(SysInboxEventEntity::getSourceSystem, sourceSystem);
        }
        if (objectType != null && !objectType.isBlank()) {
            countQuery.eq(SysInboxEventEntity::getObjectType, objectType);
            query.eq(SysInboxEventEntity::getObjectType, objectType);
        }
        if (from != null) {
            countQuery.ge(SysInboxEventEntity::getReceivedAt, from);
            query.ge(SysInboxEventEntity::getReceivedAt, from);
        }
        if (to != null) {
            countQuery.le(SysInboxEventEntity::getReceivedAt, to);
            query.le(SysInboxEventEntity::getReceivedAt, to);
        }
        long total = inboxMapper.selectCount(countQuery);
        int offset = (page - 1) * size;
        query.orderByDesc(SysInboxEventEntity::getReceivedAt);
        return PageResult.of(inboxMapper.selectList(query.last("LIMIT " + offset + "," + size))
                .stream().map(this::toDomain).toList(), page, size, total);
    }

    @Override
    @Transactional
    public OutboxEvent createOutbox(OutboxEvent event) {
        SysOutboxEventEntity existing = findOutboxEntityByEventId(event.eventId());
        if (existing != null) return toDomain(existing);
        SysOutboxEventEntity entity = new SysOutboxEventEntity();
        entity.setEventId(event.eventId());
        entity.setObjectType(event.objectType());
        entity.setObjectId(event.objectId());
        entity.setObjectVersion(event.objectVersion());
        entity.setEventType(event.eventType());
        entity.setPayloadSummary(event.payloadSummary());
        entity.setStatus("READY");
        entity.setAttemptCount(event.attemptCount() == null ? 0 : event.attemptCount());
        entity.setMaxAttempts(event.maxAttempts() == null ? 5 : event.maxAttempts());
        entity.setTraceId(event.traceId());
        entity.setOccurredAt(event.occurredAt());
        entity.setCreatedAt(event.createdAt());
        try {
            outboxMapper.insert(entity);
            return toDomain(entity);
        } catch (DuplicateKeyException concurrentDuplicate) {
            SysOutboxEventEntity concurrent = findOutboxEntityByEventId(event.eventId());
            if (concurrent == null) throw concurrentDuplicate;
            return toDomain(concurrent);
        }
    }

    @Override
    public Optional<OutboxEvent> findOutbox(Long id) {
        return Optional.ofNullable(outboxMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean claimOutbox(Long id, LocalDateTime now) {
        return outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, id)
                .eq(SysOutboxEventEntity::getStatus, "READY")
                .apply("(next_retry_at IS NULL OR next_retry_at <= {0})", now)
                .apply("attempt_count < max_attempts")
                .set(SysOutboxEventEntity::getStatus, "PROCESSING")) == 1;
    }

    @Override
    @Transactional
    public Long releaseDueOutbox(LocalDateTime now) {
        SysOutboxEventEntity due = outboxMapper.selectOne(new LambdaQueryWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getStatus, "FAILED")
                .isNotNull(SysOutboxEventEntity::getNextRetryAt)
                .le(SysOutboxEventEntity::getNextRetryAt, now)
                .apply("attempt_count < max_attempts")
                .orderByAsc(SysOutboxEventEntity::getNextRetryAt)
                .last("LIMIT 1"));
        if (due == null) return null;
        int updated = outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, due.getId())
                .eq(SysOutboxEventEntity::getStatus, "FAILED")
                .le(SysOutboxEventEntity::getNextRetryAt, now)
                .apply("attempt_count < max_attempts")
                .set(SysOutboxEventEntity::getStatus, "READY"));
        return updated == 1 ? due.getId() : null;
    }

    @Override
    @Transactional
    public boolean markOutboxPublished(Long id) {
        return outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, id)
                .eq(SysOutboxEventEntity::getStatus, "PROCESSING")
                .set(SysOutboxEventEntity::getStatus, "PUBLISHED")
                .set(SysOutboxEventEntity::getLastError, null)
                .set(SysOutboxEventEntity::getNextRetryAt, null)) == 1;
    }

    @Override
    @Transactional
    public void markOutboxFailed(Long id, String error, LocalDateTime nextRetryAt) {
        SysOutboxEventEntity entity = outboxMapper.selectById(id);
        if (entity == null) throw new IllegalStateException("Outbox event not found: " + id);
        int attempts = entity.getAttemptCount() == null ? 1 : entity.getAttemptCount() + 1;
        int maxAttempts = entity.getMaxAttempts() == null ? 5 : entity.getMaxAttempts();
        int updated = outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, id)
                .eq(SysOutboxEventEntity::getStatus, "PROCESSING")
                .set(SysOutboxEventEntity::getStatus, attempts >= maxAttempts ? "DEAD" : "FAILED")
                .set(SysOutboxEventEntity::getAttemptCount, attempts)
                .set(SysOutboxEventEntity::getLastError, error)
                .set(SysOutboxEventEntity::getNextRetryAt, attempts >= maxAttempts ? null : nextRetryAt));
        if (updated != 1) throw new IllegalStateException("Outbox failure transition rejected: " + id);
    }

    @Override
    @Transactional
    public void markOutboxNoTransport(Long id, String reason) {
        int updated = outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, id)
                .eq(SysOutboxEventEntity::getStatus, "PROCESSING")
                .set(SysOutboxEventEntity::getStatus, "NO_TRANSPORT")
                .set(SysOutboxEventEntity::getLastError, reason));
        if (updated != 1) throw new IllegalStateException("Outbox no-transport transition rejected: " + id);
    }

    @Override
    @Transactional
    public boolean resetOutboxForRetry(Long id) {
        return outboxMapper.update(null, new LambdaUpdateWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getId, id)
                .eq(SysOutboxEventEntity::getStatus, "FAILED")
                .apply("attempt_count < max_attempts")
                .set(SysOutboxEventEntity::getStatus, "READY")
                .set(SysOutboxEventEntity::getNextRetryAt, null)) == 1;
    }

    @Override
    public PageResult<OutboxEvent> listOutbox(int page, int size, String status, String eventType,
                                              String objectType, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<SysOutboxEventEntity> countQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysOutboxEventEntity> query = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            countQuery.eq(SysOutboxEventEntity::getStatus, status);
            query.eq(SysOutboxEventEntity::getStatus, status);
        }
        if (eventType != null && !eventType.isBlank()) {
            countQuery.eq(SysOutboxEventEntity::getEventType, eventType);
            query.eq(SysOutboxEventEntity::getEventType, eventType);
        }
        if (objectType != null && !objectType.isBlank()) {
            countQuery.eq(SysOutboxEventEntity::getObjectType, objectType);
            query.eq(SysOutboxEventEntity::getObjectType, objectType);
        }
        if (from != null) {
            countQuery.ge(SysOutboxEventEntity::getOccurredAt, from);
            query.ge(SysOutboxEventEntity::getOccurredAt, from);
        }
        if (to != null) {
            countQuery.le(SysOutboxEventEntity::getOccurredAt, to);
            query.le(SysOutboxEventEntity::getOccurredAt, to);
        }
        long total = outboxMapper.selectCount(countQuery);
        int offset = (page - 1) * size;
        query.orderByDesc(SysOutboxEventEntity::getCreatedAt);
        return PageResult.of(outboxMapper.selectList(query.last("LIMIT " + offset + "," + size))
                .stream().map(this::toDomain).toList(), page, size, total);
    }

    private void requireInboxTransition(Long id, String from, String to, LocalDateTime processedAt,
                                        String result, String error, LocalDateTime nextRetryAt) {
        int updated = inboxMapper.update(null, new LambdaUpdateWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getId, id)
                .eq(SysInboxEventEntity::getStatus, from)
                .set(SysInboxEventEntity::getStatus, to)
                .set(SysInboxEventEntity::getProcessedAt, processedAt)
                .set(SysInboxEventEntity::getResultMessage, result)
                .set(SysInboxEventEntity::getLastError, error)
                .set(SysInboxEventEntity::getNextRetryAt, nextRetryAt)
                .set(SysInboxEventEntity::getProcessingStartedAt, null));
        if (updated != 1) throw new IllegalStateException("Inbox transition rejected: " + id);
    }

    private SysInboxEventEntity findInboxEntityByEventId(String eventId) {
        return inboxMapper.selectOne(new LambdaQueryWrapper<SysInboxEventEntity>()
                .eq(SysInboxEventEntity::getEventId, eventId));
    }

    private SysOutboxEventEntity findOutboxEntityByEventId(String eventId) {
        return outboxMapper.selectOne(new LambdaQueryWrapper<SysOutboxEventEntity>()
                .eq(SysOutboxEventEntity::getEventId, eventId));
    }

    private InboxEvent toDomain(SysInboxEventEntity entity) {
        return new InboxEvent(entity.getId(), entity.getEventId(), entity.getSourceSystem(),
                entity.getObjectType(), entity.getObjectId(), entity.getObjectVersion(),
                entity.getPayloadSummary(), entity.getStatus(), entity.getResultMessage(),
                entity.getAttemptCount(), entity.getMaxAttempts(), entity.getNextRetryAt(),
                entity.getLastError(), entity.getReceivedAt(), entity.getProcessedAt());
    }

    private OutboxEvent toDomain(SysOutboxEventEntity entity) {
        return new OutboxEvent(entity.getId(), entity.getEventId(), entity.getObjectType(),
                entity.getObjectId(), entity.getObjectVersion(), entity.getEventType(),
                entity.getPayloadSummary(), entity.getStatus(), entity.getAttemptCount(),
                entity.getMaxAttempts(), entity.getNextRetryAt(), entity.getLastError(),
                entity.getTraceId(), entity.getOccurredAt(), entity.getCreatedAt());
    }
}
