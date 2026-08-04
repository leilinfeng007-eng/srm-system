package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record Role(
        Long id,
        String roleCode,
        String roleName,
        String description,
        String status,
        Boolean builtIn,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Long version) {
}
