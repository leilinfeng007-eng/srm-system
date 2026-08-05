package com.srm.system.domain.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.domain.model.Role;
import com.srm.system.domain.repository.RoleRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RoleTransferGuard {

    private final RoleRepository roleRepository;

    public RoleTransferGuard(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void requireRoleEnabled(Role role) {
        if (!"ACTIVE".equals(role.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "角色【" + role.roleName() + "】已停用，不能分配");
        }
    }

    public void requireTransferable(Role role, Set<String> actorPermissions) {
        if ("SUPER_ADMIN".equals(role.roleCode()) && !actorPermissions.contains("ROLE_SUPER_ADMIN")) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "只有超级管理员才能授予超级管理员角色");
        }
        List<String> rolePermissions = roleRepository.findPermissionCodesByRole(role.id());
        for (String permission : rolePermissions) {
            if (!actorPermissions.contains(permission)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "不能授予超出自己管理能力的角色【" + role.roleName() + "】");
            }
        }
    }

    public void requireSuperAdminAdministration(Role role, boolean actorIsSuperAdmin) {
        if (role != null && "SUPER_ADMIN".equals(role.roleCode()) && !actorIsSuperAdmin) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "只有超级管理员才能变更超级管理员授权");
        }
    }

    public void requireNotSelfAssignment(Long targetUserId, Long actorUserId, boolean actorIsSuperAdmin) {
        if (actorUserId != null && actorUserId.equals(targetUserId) && !actorIsSuperAdmin) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "不能给自己增加角色");
        }
    }

    public boolean canGrant(Role role, Set<String> actorPermissions) {
        if ("SUPER_ADMIN".equals(role.roleCode())) {
            return actorPermissions.contains("ROLE_SUPER_ADMIN");
        }
        for (String permission : roleRepository.findPermissionCodesByRole(role.id())) {
            if (!actorPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}
