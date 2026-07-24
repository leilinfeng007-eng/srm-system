package com.srm.security.token;

import java.time.Instant;
import java.util.Optional;

public interface TokenSessionRepository {

    long create(
            long userId,
            String tokenHash,
            String accessJti,
            Instant expiresAt,
            String deviceInfo);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    boolean isAccessActive(long sessionId, long userId, String accessJti, Instant now);

    boolean rotate(
            long sessionId,
            String previousTokenHash,
            String nextTokenHash,
            String accessJti,
            Instant expiresAt,
            Instant now);

    void revokeById(long sessionId, Instant now);

    void revokeByTokenHash(String tokenHash, Instant now);
}
