package com.srm.system.api.response;

import java.time.LocalDateTime;
import java.util.List;

public record RoleDetailResponse(
        Long id,
        String roleCode,
        String roleName,
        String description,
        String status,
        Boolean builtIn,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Long> userIds,
        List<Long> menuIds,
        List<Long> permissionIds,
        List<RoleDataPolicyResponse> dataPolicies,
        List<RoleHistoryResponse> history) {
}
