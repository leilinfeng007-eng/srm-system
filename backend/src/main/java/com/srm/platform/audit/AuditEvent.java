package com.srm.platform.audit;

import java.time.Instant;

public record AuditEvent(
        String action,
        String actor,
        String targetType,
        String targetId,
        String result,
        String traceId,
        Instant occurredAt) {
}

