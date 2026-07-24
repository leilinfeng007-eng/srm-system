package com.srm.security.token;

import java.time.Instant;

public record JwtClaims(
        long userId,
        long sessionId,
        String username,
        String jti,
        Instant expiresAt) {
}

