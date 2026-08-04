package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record InboxEvent(
        Long id,
        String eventId,
        String sourceSystem,
        String objectType,
        String objectId,
        Long objectVersion,
        String payload,
        String status,
        String resultMessage,
        Integer attemptCount,
        Integer maxAttempts,
        LocalDateTime nextRetryAt,
        String lastError,
        LocalDateTime receivedAt,
        LocalDateTime processedAt) {
}
