package com.srm.security.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.srm.security.infrastructure.persistence.entity.SysRefreshTokenEntity;
import com.srm.security.infrastructure.persistence.mapper.TokenSessionMapper;
import com.srm.security.token.RefreshSession;
import com.srm.security.token.TokenSessionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisTokenSessionRepository implements TokenSessionRepository {

    private final TokenSessionMapper sessions;

    public MybatisTokenSessionRepository(TokenSessionMapper sessions) {
        this.sessions = sessions;
    }

    @Override
    public long create(
            long userId,
            String tokenHash,
            String accessJti,
            Instant expiresAt,
            String deviceInfo) {
        SysRefreshTokenEntity entity = new SysRefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setAccessJti(accessJti);
        entity.setExpiresAt(expiresAt);
        entity.setDeviceInfo(deviceInfo);
        if (sessions.insert(entity) != 1 || entity.getId() == null) {
            throw new IllegalStateException("Database did not return a generated refresh session id");
        }
        return entity.getId();
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        SysRefreshTokenEntity entity = sessions.selectOne(
                Wrappers.<SysRefreshTokenEntity>lambdaQuery()
                        .eq(SysRefreshTokenEntity::getTokenHash, tokenHash));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public boolean isAccessActive(long sessionId, long userId, String accessJti, Instant now) {
        return sessions.countActiveAccess(sessionId, userId, accessJti, now) == 1;
    }

    @Override
    public boolean rotate(
            long sessionId,
            String previousTokenHash,
            String nextTokenHash,
            String accessJti,
            Instant expiresAt,
            Instant now) {
        LambdaUpdateWrapper<SysRefreshTokenEntity> update = Wrappers
                .<SysRefreshTokenEntity>lambdaUpdate()
                .eq(SysRefreshTokenEntity::getId, sessionId)
                .eq(SysRefreshTokenEntity::getTokenHash, previousTokenHash)
                .isNull(SysRefreshTokenEntity::getRevokedAt)
                .gt(SysRefreshTokenEntity::getExpiresAt, now)
                .set(SysRefreshTokenEntity::getTokenHash, nextTokenHash)
                .set(SysRefreshTokenEntity::getAccessJti, accessJti)
                .set(SysRefreshTokenEntity::getExpiresAt, expiresAt)
                .set(SysRefreshTokenEntity::getLastUsedAt, now);
        return sessions.update(null, update) == 1;
    }

    @Override
    public void revokeById(long sessionId, Instant now) {
        sessions.update(null, Wrappers.<SysRefreshTokenEntity>lambdaUpdate()
                .eq(SysRefreshTokenEntity::getId, sessionId)
                .isNull(SysRefreshTokenEntity::getRevokedAt)
                .set(SysRefreshTokenEntity::getRevokedAt, now));
    }

    @Override
    public void revokeByTokenHash(String tokenHash, Instant now) {
        sessions.update(null, Wrappers.<SysRefreshTokenEntity>lambdaUpdate()
                .eq(SysRefreshTokenEntity::getTokenHash, tokenHash)
                .isNull(SysRefreshTokenEntity::getRevokedAt)
                .set(SysRefreshTokenEntity::getRevokedAt, now));
    }

    private RefreshSession toDomain(SysRefreshTokenEntity entity) {
        return new RefreshSession(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getAccessJti(),
                entity.getExpiresAt(),
                entity.getRevokedAt());
    }
}
