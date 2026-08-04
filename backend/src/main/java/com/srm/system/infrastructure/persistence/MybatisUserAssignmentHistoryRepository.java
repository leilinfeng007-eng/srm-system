package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.model.UserAssignmentHistory;
import com.srm.system.domain.repository.UserAssignmentHistoryRepository;
import com.srm.system.infrastructure.persistence.entity.SysUserAssignmentHistoryEntity;
import com.srm.system.infrastructure.persistence.mapper.UserAssignmentHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserAssignmentHistoryRepository implements UserAssignmentHistoryRepository {

    private final UserAssignmentHistoryMapper mapper;

    public MybatisUserAssignmentHistoryRepository(UserAssignmentHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserAssignmentHistory save(UserAssignmentHistory history) {
        SysUserAssignmentHistoryEntity entity = toEntity(history);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<UserAssignmentHistory> findByUserId(Long userId) {
        LambdaQueryWrapper<SysUserAssignmentHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAssignmentHistoryEntity::getUserId, userId)
               .orderByDesc(SysUserAssignmentHistoryEntity::getChangedAt);
        List<SysUserAssignmentHistoryEntity> entities = mapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private UserAssignmentHistory toDomain(SysUserAssignmentHistoryEntity entity) {
        return new UserAssignmentHistory(
                entity.getId(),
                entity.getUserId(),
                entity.getFieldName(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getChangeReason(),
                entity.getChangedBy(),
                entity.getChangedAt());
    }

    private SysUserAssignmentHistoryEntity toEntity(UserAssignmentHistory history) {
        SysUserAssignmentHistoryEntity entity = new SysUserAssignmentHistoryEntity();
        entity.setId(history.id());
        entity.setUserId(history.userId());
        entity.setFieldName(history.fieldName());
        entity.setOldValue(history.oldValue());
        entity.setNewValue(history.newValue());
        entity.setChangeReason(history.changeReason());
        entity.setChangedBy(history.changedBy());
        entity.setChangedAt(history.changedAt());
        return entity;
    }
}
