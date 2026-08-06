package com.srm.system.api.response;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String deptCode,
        String deptName,
        Long organizationId,
        String organizationName,
        Long parentId,
        String parentName,
        Long managerId,
        String managerName,
        Integer sortOrder,
        String path,
        Integer level,
        String status,
        String description,
        long positionCount,
        long userCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
