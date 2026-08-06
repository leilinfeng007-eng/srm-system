package com.srm.system.api.request;

import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @Size(max = 100) String deptName,
        Long organizationId,
        Long parentId,
        Long managerId,
        @Size(max = 100) String managerName,
        Integer sortOrder,
        @Size(max = 255) String description,
        Long version) {
}
