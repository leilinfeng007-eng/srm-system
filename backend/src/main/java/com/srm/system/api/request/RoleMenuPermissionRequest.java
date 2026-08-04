package com.srm.system.api.request;

import java.util.List;

public record RoleMenuPermissionRequest(
        List<Long> menuIds,
        List<Long> permissionIds) {
}
