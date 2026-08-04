package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.model.UserRoleHistory;
import com.srm.system.domain.repository.UserRoleHistoryRepository;
import com.srm.system.infrastructure.persistence.entity.SysUserRoleHistoryEntity;
import com.srm.system.infrastructure.persistence.mapper.UserRoleHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserRoleHistoryRepository implements UserRoleHistoryRepository {

    private final UserRoleHistoryMapper mapper;

    public MybatisUserRoleHistoryRepository(UserRoleHistoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserRoleHistory save(UserRoleHistory history) {
        SysUserRoleHistoryEntity entity = toEntity(history);
        mapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<UserRoleHistory> findByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRoleHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRoleHistoryEntity::getUserId, userId)
               .orderByDesc(SysUserRoleHistoryEntity::getChangedAt);
        List<SysUserRoleHistoryEntity> entities = mapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserRoleHistory> findByRoleId(Long roleId) {
        LambdaQueryWrapper<SysUserRoleHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRoleHistoryEntity::getRoleId, roleId)
               .orderByDesc(SysUserRoleHistoryEntity::getChangedAt);
        List<SysUserRoleHistoryEntity> entities = mapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    private UserRoleHistory toDomain(SysUserRoleHistoryEntity entity) {
        return new UserRoleHistory(
                entity.getId(),
                entity.getUserId(),
                entity.getRoleId(),
                entity.getAction(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getChangedBy(),
                entity.getChangedAt());
    }

    private SysUserRoleHistoryEntity toEntity(UserRoleHistory history) {
        SysUserRoleHistoryEntity entity = new SysUserRoleHistoryEntity();
        entity.setId(history.id());
        entity.setUserId(history.userId());
        entity.setRoleId(history.roleId());
        entity.setAction(history.action());
        entity.setPreviousStatus(history.previousStatus());
        entity.setNewStatus(history.newStatus());
        entity.setChangedBy(history.changedBy());
        entity.setChangedAt(history.changedAt());
        return entity;
    }
}
