package com.srm.system.api.response;

import java.util.List;

public record UserEffectivePermissionsResponse(
        Long userId, List<EffectivePermissionResponse> permissions) {
    public UserEffectivePermissionsResponse {
        permissions = List.copyOf(permissions);
    }
}
