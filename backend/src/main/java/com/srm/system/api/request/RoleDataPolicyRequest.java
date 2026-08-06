package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;

public record RoleDataPolicyRequest(
        @NotBlank String domainCode,
        @NotBlank String dimensionCode,
        @NotBlank String scopeType,
        @NotBlank String operationMode,
        boolean includeChildren,
        String scopeOrgIds) {
}
