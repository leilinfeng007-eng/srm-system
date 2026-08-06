package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record Department(
        Long id,
        String deptCode,
        String deptName,
        Long organizationId,
        Long parentId,
        String managerName,
        Long managerId,
        Integer sortOrder,
        String path,
        Integer level,
        String status,
        String description,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Long version) {
}
