package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.security.infrastructure.persistence.entity.SysOperationLogEntity;
import com.srm.security.infrastructure.persistence.mapper.OperationLogMapper;
import com.srm.system.domain.model.UserOperationLogEntry;
import com.srm.system.domain.repository.UserOperationLogRepository;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserOperationLogRepository implements UserOperationLogRepository {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public MybatisUserOperationLogRepository(OperationLogMapper operationLogMapper,
                                             ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UserOperationLogEntry> findByTargetUser(Long userId, List<String> actionCodes) {
        List<SysOperationLogEntity> entities = operationLogMapper.selectList(
                new LambdaQueryWrapper<SysOperationLogEntity>()
                        .eq(SysOperationLogEntity::getTargetType, "USER")
                        .eq(SysOperationLogEntity::getTargetId, String.valueOf(userId))
                        .in(SysOperationLogEntity::getActionCode, actionCodes));
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(entity -> new UserOperationLogEntry(
                entity.getActionCode(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getResultCode(),
                extractField(entity.getFieldChanges(), "before"),
                extractField(entity.getFieldChanges(), "after"),
                entity.getOperatorName() != null ? entity.getOperatorName() : "system",
                entity.getOccurredAt())).collect(Collectors.toList());
    }

    private String extractField(String fieldChanges, String name) {
        if (fieldChanges == null || fieldChanges.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(fieldChanges);
            return node.path(name).asText(null);
        } catch (Exception ignored) {
            return fieldChanges;
        }
    }
}
