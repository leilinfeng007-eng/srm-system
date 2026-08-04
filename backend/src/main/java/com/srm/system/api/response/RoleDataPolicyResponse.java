package com.srm.system.api.response;

public record RoleDataPolicyResponse(
        String domainCode,
        String dimensionCode,
        String scopeType,
        boolean includeChildren,
        String operationMode) {
}
