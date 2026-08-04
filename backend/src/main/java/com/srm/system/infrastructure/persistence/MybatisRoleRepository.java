package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.srm.security.infrastructure.persistence.entity.SysUserRoleEntity;
import com.srm.security.infrastructure.persistence.mapper.UserRoleMapper;
import com.srm.system.domain.model.Role;
import com.srm.system.domain.repository.RoleRepository;
import com.srm.system.domain.repository.DataPolicyRequest;
import com.srm.system.infrastructure.persistence.entity.SysRoleEntity;
import com.srm.system.infrastructure.persistence.entity.SysRoleDataPolicyEntity;
import com.srm.system.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.srm.system.infrastructure.persistence.entity.SysRolePermissionEntity;
import com.srm.system.infrastructure.persistence.mapper.SysRoleMapper;
import com.srm.system.infrastructure.persistence.mapper.SysRoleDataPolicyMapper;
import com.srm.system.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.srm.system.infrastructure.persistence.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRoleRepository implements RoleRepository {

    private final SysRoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleDataPolicyMapper dataPolicyMapper;

    public MybatisRoleRepository(SysRoleMapper roleMapper,
                                  UserRoleMapper userRoleMapper,
                                  SysRoleMenuMapper roleMenuMapper,
                                  SysRolePermissionMapper rolePermissionMapper,
                                  SysRoleDataPolicyMapper dataPolicyMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.dataPolicyMapper = dataPolicyMapper;
    }

    @Override
    public Optional<Role> findById(Long id) {
        SysRoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public Role save(Role role) {
        SysRoleEntity entity = toEntity(role);
        roleMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Role update(Role role) {
        SysRoleEntity entity = toEntity(role);
        roleMapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public boolean updateStatus(Long id, String status, Long version) {
        LambdaUpdateWrapper<SysRoleEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRoleEntity::getId, id)
               .eq(SysRoleEntity::getVersion, version)
               .set(SysRoleEntity::getStatus, status)
               .setSql("version = version + 1");
        return roleMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleEntity::getRoleCode, roleCode);
        return roleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countActiveUsers(Long roleId) {
        return roleMapper.countActiveUsers(roleId);
    }

    @Override
    public long countActiveUsersExcluding(Long roleId, Long excludeUserId) {
        return roleMapper.countActiveUsersExcluding(roleId, excludeUserId);
    }

    @Override
    public List<Long> findUserIdsByRole(Long roleId) {
        List<SysUserRoleEntity> assignments = userRoleMapper.selectList(
                Wrappers.<SysUserRoleEntity>lambdaQuery()
                        .eq(SysUserRoleEntity::getRoleId, roleId)
                        .eq(SysUserRoleEntity::getStatus, "ACTIVE"));
        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }
        return assignments.stream().map(SysUserRoleEntity::getUserId).collect(Collectors.toList());
    }

    @Override
    public List<Long> findMenuIdsByRole(Long roleId) {
        List<SysRoleMenuEntity> menus = roleMenuMapper.selectList(
                Wrappers.<SysRoleMenuEntity>lambdaQuery().eq(SysRoleMenuEntity::getRoleId, roleId));
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }
        return menus.stream().map(SysRoleMenuEntity::getMenuId).collect(Collectors.toList());
    }

    @Override
    public List<Long> findPermissionIdsByRole(Long roleId) {
        List<SysRolePermissionEntity> perms = rolePermissionMapper.selectList(
                Wrappers.<SysRolePermissionEntity>lambdaQuery().eq(SysRolePermissionEntity::getRoleId, roleId));
        if (perms == null || perms.isEmpty()) {
            return Collections.emptyList();
        }
        return perms.stream().map(SysRolePermissionEntity::getPermissionId).collect(Collectors.toList());
    }

    @Override
    public void assignUserToRole(Long userId, Long roleId, String actor) {
        SysUserRoleEntity existing = userRoleMapper.selectOne(
                Wrappers.<SysUserRoleEntity>lambdaQuery()
                        .eq(SysUserRoleEntity::getUserId, userId)
                        .eq(SysUserRoleEntity::getRoleId, roleId));
        if (existing == null) {
            SysUserRoleEntity assignment = new SysUserRoleEntity();
            assignment.setUserId(userId);
            assignment.setRoleId(roleId);
            assignment.setStatus("ACTIVE");
            assignment.setCreatedBy(actor);
            userRoleMapper.insert(assignment);
        } else if (!"ACTIVE".equals(existing.getStatus())) {
            existing.setStatus("ACTIVE");
            existing.setEffectiveTo(null);
            userRoleMapper.updateById(existing);
        }
    }

    @Override
    public void removeUserFromRole(Long userId, Long roleId) {
        SysUserRoleEntity assignment = userRoleMapper.selectOne(
                Wrappers.<SysUserRoleEntity>lambdaQuery()
                        .eq(SysUserRoleEntity::getUserId, userId)
                        .eq(SysUserRoleEntity::getRoleId, roleId)
                        .eq(SysUserRoleEntity::getStatus, "ACTIVE"));
        if (assignment != null) {
            assignment.setStatus("INACTIVE");
            userRoleMapper.updateById(assignment);
        }
    }

    @Override
    public List<Role> findPage(int offset, int limit) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = Wrappers.<SysRoleEntity>lambdaQuery()
                .orderByAsc(SysRoleEntity::getId)
                .last("LIMIT " + offset + "," + limit);
        List<SysRoleEntity> entities = roleMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return roleMapper.selectCount(null);
    }

    @Override
    public void clearRoleMenus(Long roleId, String actor) {
        roleMenuMapper.delete(Wrappers.<SysRoleMenuEntity>lambdaQuery().eq(SysRoleMenuEntity::getRoleId, roleId));
    }

    @Override
    public void clearRolePermissions(Long roleId, String actor) {
        rolePermissionMapper.delete(Wrappers.<SysRolePermissionEntity>lambdaQuery().eq(SysRolePermissionEntity::getRoleId, roleId));
    }

    @Override
    public void addRoleMenus(Long roleId, List<Long> menuIds, String actor) {
        for (Long menuId : menuIds) {
            SysRoleMenuEntity rm = new SysRoleMenuEntity();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            rm.setCreatedBy(actor);
            roleMenuMapper.insert(rm);
        }
    }

    @Override
    public void addRolePermissions(Long roleId, List<Long> permissionIds, String actor) {
        for (Long permissionId : permissionIds) {
            SysRolePermissionEntity rp = new SysRolePermissionEntity();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setCreatedBy(actor);
            rolePermissionMapper.insert(rp);
        }
    }

    @Override
    public List<DataPolicyRequest> findDataPolicies(Long roleId) {
        return dataPolicyMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleDataPolicyEntity>()
                .eq(SysRoleDataPolicyEntity::getRoleId, roleId)).stream()
                .map(p -> new DataPolicyRequest(p.getDomainCode(), p.getDimensionCode(),
                        p.getScopeType(), Boolean.TRUE.equals(p.getIncludeChildren()), p.getOperationMode()))
                .collect(Collectors.toList());
    }

    @Override
    public void replaceDataPolicies(Long roleId, List<DataPolicyRequest> policies, String actor) {
        dataPolicyMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleDataPolicyEntity>()
                .eq(SysRoleDataPolicyEntity::getRoleId, roleId));
        if (policies != null) {
            for (DataPolicyRequest policy : policies) {
                SysRoleDataPolicyEntity entity = new SysRoleDataPolicyEntity();
                entity.setRoleId(roleId);
                entity.setDomainCode(policy.domainCode());
                entity.setDimensionCode(policy.dimensionCode());
                entity.setScopeType(policy.scopeType());
                entity.setIncludeChildren(policy.includeChildren());
                entity.setOperationMode(policy.operationMode());
                entity.setStatus("ACTIVE");
                entity.setCreatedBy(actor);
                entity.setUpdatedBy(actor);
                dataPolicyMapper.insert(entity);
            }
        }
    }

    private Role toDomain(SysRoleEntity entity) {
        return new Role(
                entity.getId(), entity.getRoleCode(), entity.getRoleName(),
                entity.getDescription(), entity.getStatus(), entity.getBuiltIn(),
                entity.getCreatedBy(), entity.getCreatedAt(),
                entity.getUpdatedBy(), entity.getUpdatedAt(), entity.getVersion());
    }

    private SysRoleEntity toEntity(Role role) {
        SysRoleEntity entity = new SysRoleEntity();
        entity.setId(role.id());
        entity.setRoleCode(role.roleCode());
        entity.setRoleName(role.roleName());
        entity.setDescription(role.description());
        entity.setStatus(role.status());
        entity.setBuiltIn(role.builtIn());
        entity.setCreatedBy(role.createdBy());
        entity.setCreatedAt(role.createdAt());
        entity.setUpdatedBy(role.updatedBy());
        entity.setUpdatedAt(role.updatedAt());
        entity.setVersion(role.version());
        return entity;
    }
}
