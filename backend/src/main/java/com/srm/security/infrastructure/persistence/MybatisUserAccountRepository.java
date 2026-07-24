package com.srm.security.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.srm.platform.navigation.NavigationCacheInvalidator;
import com.srm.security.auth.SrmPrincipal;
import com.srm.security.auth.UserAccountRepository;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import com.srm.security.infrastructure.persistence.entity.SysUserRoleEntity;
import com.srm.security.infrastructure.persistence.mapper.UserAccountMapper;
import com.srm.security.infrastructure.persistence.mapper.UserRoleMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper users;
    private final UserRoleMapper userRoles;
    private final NavigationCacheInvalidator navigationCache;

    public MybatisUserAccountRepository(
            UserAccountMapper users,
            UserRoleMapper userRoles,
            NavigationCacheInvalidator navigationCache) {
        this.users = users;
        this.userRoles = userRoles;
        this.navigationCache = navigationCache;
    }

    @Override
    public Optional<SrmPrincipal> findByUsername(String username) {
        SysUserEntity entity = users.selectOne(Wrappers.<SysUserEntity>lambdaQuery()
                .eq(SysUserEntity::getUsername, username));
        return Optional.ofNullable(entity).map(this::toPrincipal);
    }

    @Override
    public Optional<SrmPrincipal> findById(long userId) {
        return Optional.ofNullable(users.selectById(userId)).map(this::toPrincipal);
    }

    @Override
    public long createUserIfMissing(
            String username,
            String passwordHash,
            String displayName,
            String actor) {
        Optional<SrmPrincipal> existing = findByUsername(username);
        if (existing.isPresent()) {
            return existing.get().userId();
        }
        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        entity.setDisplayName(displayName);
        entity.setStatus("ACTIVE");
        entity.setCreatedBy(actor);
        entity.setUpdatedBy(actor);
        if (users.insert(entity) != 1 || entity.getId() == null) {
            throw new IllegalStateException("Database did not return a generated user id");
        }
        return entity.getId();
    }

    @Override
    public void assignRoleIfMissing(long userId, String roleCode, String actor) {
        Long roleId = userRoles.selectActiveRoleId(roleCode);
        if (roleId == null) {
            throw new IllegalStateException("Required role is missing: " + roleCode);
        }
        if (userRoles.countAssignment(userId, roleId) == 0) {
            SysUserRoleEntity assignment = new SysUserRoleEntity();
            assignment.setUserId(userId);
            assignment.setRoleId(roleId);
            assignment.setCreatedBy(actor);
            if (userRoles.insert(assignment) != 1) {
                throw new IllegalStateException("Unable to assign required role: " + roleCode);
            }
            navigationCache.evictUser(userId);
        }
    }

    @Override
    public void markLogin(long userId, Instant loginAt) {
        users.updateLogin(userId, loginAt, "authentication");
    }

    private SrmPrincipal toPrincipal(SysUserEntity user) {
        return new SrmPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getStatus(),
                users.selectActiveRoles(user.getId()),
                users.selectEnabledPermissions(user.getId()),
                null);
    }
}
