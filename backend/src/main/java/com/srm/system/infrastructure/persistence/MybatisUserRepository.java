package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import com.srm.security.infrastructure.persistence.mapper.TokenSessionMapper;
import com.srm.security.infrastructure.persistence.mapper.UserAccountMapper;
import com.srm.system.domain.model.User;
import com.srm.system.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MybatisUserRepository implements UserRepository {

    private final UserAccountMapper userAccountMapper;
    private final TokenSessionMapper tokenSessionMapper;

    public MybatisUserRepository(UserAccountMapper userAccountMapper,
                                  TokenSessionMapper tokenSessionMapper) {
        this.userAccountMapper = userAccountMapper;
        this.tokenSessionMapper = tokenSessionMapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        SysUserEntity entity = userAccountMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        SysUserEntity entity = userAccountMapper.selectOne(
                Wrappers.<SysUserEntity>lambdaQuery().eq(SysUserEntity::getUsername, username));
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public User save(User user) {
        SysUserEntity entity = toEntity(user);
        userAccountMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public User update(User user) {
        SysUserEntity entity = toEntity(user);
        userAccountMapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public long countByUsername(String username) {
        return userAccountMapper.selectCount(
                Wrappers.<SysUserEntity>lambdaQuery().eq(SysUserEntity::getUsername, username));
    }

    @Override
    public List<String> findActiveRoleCodes(Long userId) {
        return userAccountMapper.selectActiveRoles(userId);
    }

    @Override
    public List<User> findPage(String username, String displayName, String status,
                               List<Long> allowedOrganizationIds, boolean allOrganizations,
                               int offset, int limit) {
        LambdaQueryWrapper<SysUserEntity> wrapper = Wrappers.<SysUserEntity>lambdaQuery();
        applyOrganizationScope(wrapper, allowedOrganizationIds, allOrganizations);
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUserEntity::getUsername, username);
        }
        if (StringUtils.hasText(displayName)) {
            wrapper.like(SysUserEntity::getDisplayName, displayName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUserEntity::getStatus, status);
        }
        wrapper.orderByAsc(SysUserEntity::getId);
        wrapper.last("LIMIT " + offset + "," + limit);
        List<SysUserEntity> entities = userAccountMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long count(String username, String displayName, String status,
                      List<Long> allowedOrganizationIds, boolean allOrganizations) {
        LambdaQueryWrapper<SysUserEntity> wrapper = Wrappers.<SysUserEntity>lambdaQuery();
        applyOrganizationScope(wrapper, allowedOrganizationIds, allOrganizations);
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUserEntity::getUsername, username);
        }
        if (StringUtils.hasText(displayName)) {
            wrapper.like(SysUserEntity::getDisplayName, displayName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUserEntity::getStatus, status);
        }
        return userAccountMapper.selectCount(wrapper);
    }

    private void applyOrganizationScope(LambdaQueryWrapper<SysUserEntity> wrapper,
                                        List<Long> allowedOrganizationIds,
                                        boolean allOrganizations) {
        if (!allOrganizations) {
            if (allowedOrganizationIds == null || allowedOrganizationIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(SysUserEntity::getMainOrganizationId, allowedOrganizationIds);
            }
        }
    }

    @Override
    public boolean updatePassword(Long userId, String passwordHash, boolean mustChange, String actor) {
        return userAccountMapper.updatePassword(userId, passwordHash, mustChange, actor) > 0;
    }

    @Override
    public boolean updateStatus(Long userId, String status) {
        SysUserEntity entity = userAccountMapper.selectById(userId);
        if (entity == null) {
            return false;
        }
        entity.setStatus(status);
        return userAccountMapper.updateById(entity) > 0;
    }

    @Override
    public void revokeSessions(Long userId) {
        tokenSessionMapper.revokeByUserId(userId, Instant.now());
    }

    private User toDomain(SysUserEntity entity) {
        return new User(
                entity.getId(), entity.getUsername(), entity.getPasswordHash(),
                entity.getDisplayName(), entity.getEmployeeCode(), entity.getEmail(),
                entity.getPhone(), entity.getMainOrganizationId(),
                entity.getMainDepartmentId(), entity.getMainPositionId(),
                entity.getMustChangePassword(), entity.getStatus(),
                entity.getLastLoginAt(), entity.getCreatedAt(),
                entity.getCreatedBy(), entity.getUpdatedBy(), entity.getUpdatedAt(),
                entity.getVersion());
    }

    private SysUserEntity toEntity(User user) {
        SysUserEntity entity = new SysUserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setPasswordHash(user.passwordHash());
        entity.setDisplayName(user.displayName());
        entity.setEmployeeCode(user.employeeCode());
        entity.setEmail(user.email());
        entity.setPhone(user.phone());
        entity.setMainOrganizationId(user.mainOrganizationId());
        entity.setMainDepartmentId(user.mainDepartmentId());
        entity.setMainPositionId(user.mainPositionId());
        entity.setMustChangePassword(user.mustChangePassword());
        entity.setStatus(user.status());
        entity.setLastLoginAt(user.lastLoginAt());
        entity.setCreatedAt(user.createdAt());
        entity.setCreatedBy(user.createdBy());
        entity.setUpdatedBy(user.updatedBy());
        entity.setUpdatedAt(user.updatedAt());
        entity.setVersion(user.version());
        return entity;
    }
}
