package com.srm.system.infrastructure.persistence.dto;

public record DataScopeGrantRow(
        Long roleId,
        String roleCode,
        String scopeType,
        Boolean includeChildren,
        String operationMode,
        String scopeOrgIds) {
}
