package com.srm.system.api.response;

import java.util.List;

public record UserEffectivePermissionsView(
        Long userId,
        String username,
        String status,
        Integer effectiveRoleCount,
        Integer accessibleMenuCount,
        Integer permissionCount,
        String organizationScopeLabel,
        List<UserEffectivePermissionItem> permissions,
        List<RoleDataScopeView> roleScopes) {

    public UserEffectivePermissionsView {
        permissions = List.copyOf(permissions);
        roleScopes = List.copyOf(roleScopes);
    }

    public record UserEffectivePermissionItem(
            String permissionCode,
            String domainCode,
            String resourceCode,
            String actionCode,
            List<String> sourceRoles) {

        public UserEffectivePermissionItem {
            sourceRoles = List.copyOf(sourceRoles);
        }
    }

    public record RoleDataScopeView(
            Long roleId,
            String roleCode,
            String roleName,
            List<DataScopePolicyView> policies) {

        public RoleDataScopeView {
            policies = List.copyOf(policies);
        }
    }

    public record DataScopePolicyView(
            String domainCode,
            String dimensionCode,
            String scopeType,
            String scopeLabel,
            Boolean includeChildren,
            String operationMode,
            Boolean effective,
            String ineffectiveReason) {
    }
}
