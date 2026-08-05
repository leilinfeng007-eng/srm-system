package com.srm.system.api.response;

public record UserRoleSelectionResponse(
        Long roleId,
        String roleCode,
        String roleName,
        String status,
        Boolean builtIn,
        Boolean assigned,
        Boolean assignable) {
}
