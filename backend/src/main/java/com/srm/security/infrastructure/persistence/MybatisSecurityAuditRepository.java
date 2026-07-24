package com.srm.security.infrastructure.persistence;

import com.srm.common.web.TraceContext;
import com.srm.security.auth.SecurityAuditRepository;
import com.srm.security.infrastructure.persistence.entity.SysLoginLogEntity;
import com.srm.security.infrastructure.persistence.entity.SysOperationLogEntity;
import com.srm.security.infrastructure.persistence.mapper.LoginLogMapper;
import com.srm.security.infrastructure.persistence.mapper.OperationLogMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisSecurityAuditRepository implements SecurityAuditRepository {

    private final LoginLogMapper loginLogs;
    private final OperationLogMapper operationLogs;

    public MybatisSecurityAuditRepository(
            LoginLogMapper loginLogs,
            OperationLogMapper operationLogs) {
        this.loginLogs = loginLogs;
        this.operationLogs = operationLogs;
    }

    @Override
    public void recordLogin(
            Long userId,
            String username,
            boolean success,
            String remoteAddress,
            String reasonCode,
            Instant occurredAt) {
        SysLoginLogEntity entity = new SysLoginLogEntity();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setSuccess(success);
        entity.setIpDigest(digest(remoteAddress));
        entity.setReasonCode(reasonCode);
        entity.setTraceId(TraceContext.currentTraceId());
        entity.setOccurredAt(occurredAt);
        loginLogs.insert(entity);
    }

    @Override
    public void recordOperation(
            Long userId,
            String actionCode,
            String targetType,
            String targetId,
            String resultCode,
            Instant occurredAt) {
        SysOperationLogEntity entity = new SysOperationLogEntity();
        entity.setUserId(userId);
        entity.setActionCode(actionCode);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setResultCode(resultCode);
        entity.setTraceId(TraceContext.currentTraceId());
        entity.setOccurredAt(occurredAt);
        operationLogs.insert(entity);
    }

    private String digest(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
