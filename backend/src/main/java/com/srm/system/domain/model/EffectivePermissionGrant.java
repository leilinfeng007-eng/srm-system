package com.srm.system.domain.model;

public record EffectivePermissionGrant(
        String permissionCode, String roleCode, String domainCode,
        String dimensionCode, String scopeType, String operationMode) {
}
