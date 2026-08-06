package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.security.infrastructure.persistence.entity.SysOperationLogEntity;
import com.srm.security.infrastructure.persistence.mapper.OperationLogMapper;
import com.srm.system.domain.service.AuditRecorder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisAuditRepository implements AuditRecorder {

    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(password|secret|token|credential|api[_-]?key|private[_-]?key|jwt)");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OperationLogMapper operationLogMapper;

    public MybatisAuditRepository(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void record(String actionCode, String targetType, String targetId, String resultCode,
                       String beforeSummary, String afterSummary, String reason) {
        SysOperationLogEntity e = new SysOperationLogEntity();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = auth != null ? auth.getName() : "system";
        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof com.srm.security.auth.SrmPrincipal p) {
            userId = p.userId();
        }
        e.setUserId(userId);
        e.setOperatorName(operator);
        e.setActionCode(actionCode);
        e.setTargetType(targetType);
        e.setTargetId(targetId);
        e.setResultCode(resultCode);
        e.setReason(reason);
        e.setTraceId(com.srm.common.web.TraceContext.currentTraceId());
        e.setOccurredAt(Instant.now());
        if (beforeSummary != null || afterSummary != null) {
            e.setFieldChanges(buildFieldChanges(beforeSummary, afterSummary));
        }
        e.setBeforeHash(hash(beforeSummary));
        e.setAfterHash(hash(afterSummary));
        operationLogMapper.insert(e);
    }

    @Transactional
    public void recordRoleDataPolicyChange(Long roleId, String beforeSummary,
                                           String afterSummary, String reason) {
        record("ROLE_DATA_POLICY_UPDATED", "ROLE", String.valueOf(roleId), "SUCCESS",
                beforeSummary, afterSummary, reason);
    }

    private String buildFieldChanges(String before, String after) {
        return "{\"before\":\"" + mask(before) + "\",\"after\":\"" + mask(after) + "\"}";
    }

    private String mask(String value) {
        if (value == null) return "";
        if (SENSITIVE.matcher(value).find()) {
            return "***MASKED***";
        }
        return value.length() > 500 ? value.substring(0, 500) + "..." : value;
    }

    private String hash(String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    public List<SysOperationLogEntity> findByTarget(String targetType, String targetId) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<SysOperationLogEntity>()
                .eq(SysOperationLogEntity::getTargetType, targetType)
                .eq(SysOperationLogEntity::getTargetId, targetId)
                .orderByDesc(SysOperationLogEntity::getOccurredAt));
    }
}
