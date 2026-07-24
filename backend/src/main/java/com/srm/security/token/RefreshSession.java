package com.srm.security.token;

import java.time.Instant;

public record RefreshSession(
        long id,
        long userId,
        String tokenHash,
        String accessJti,
        Instant expiresAt,
        Instant revokedAt) {

    public boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}

