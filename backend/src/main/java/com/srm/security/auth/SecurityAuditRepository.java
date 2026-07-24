package com.srm.security.auth;

import java.time.Instant;

public interface SecurityAuditRepository {

    void recordLogin(
            Long userId,
            String username,
            boolean success,
            String remoteAddress,
            String reasonCode,
            Instant occurredAt);

    void recordOperation(
            Long userId,
            String actionCode,
            String targetType,
            String targetId,
            String resultCode,
            Instant occurredAt);
}
