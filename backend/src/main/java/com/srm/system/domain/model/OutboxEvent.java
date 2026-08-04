package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record OutboxEvent(
        Long id,
        String eventId,
        String objectType,
        String objectId,
        Long objectVersion,
        String eventType,
        String payloadSummary,
        String status,
        Integer attemptCount,
        Integer maxAttempts,
        LocalDateTime nextRetryAt,
        String lastError,
        String traceId,
        LocalDateTime occurredAt,
        LocalDateTime createdAt) {
}
