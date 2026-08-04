package com.srm.system.api.response;

import java.util.List;

public record EffectivePermissionResponse(
        String permissionCode, List<String> sourceRoles, List<String> authorizationPaths) {
    public EffectivePermissionResponse {
        sourceRoles = List.copyOf(sourceRoles);
        authorizationPaths = List.copyOf(authorizationPaths);
    }
}
