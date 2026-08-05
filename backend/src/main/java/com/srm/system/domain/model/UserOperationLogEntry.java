package com.srm.system.domain.model;

import java.time.Instant;

public record UserOperationLogEntry(
        String actionCode,
        String targetType,
        String targetId,
        String resultCode,
        String beforeSummary,
        String afterSummary,
        String changedBy,
        Instant occurredAt) {
}
