package com.srm.system.api.response;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String roleCode,
        String roleName,
        String description,
        String status,
        Boolean builtIn,
        String roleCategory,
        long userCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
