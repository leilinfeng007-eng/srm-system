package com.srm.system.api.response;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String deptCode,
        String deptName,
        Long organizationId,
        String organizationName,
        Long parentId,
        String managerName,
        Integer sortOrder,
        String path,
        Integer level,
        String status,
        String description,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
