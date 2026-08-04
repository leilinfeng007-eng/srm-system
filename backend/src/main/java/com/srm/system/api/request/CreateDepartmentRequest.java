package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank @Size(max = 64) String deptCode,
        @NotBlank @Size(max = 100) String deptName,
        @NotNull Long organizationId,
        Long parentId,
        @Size(max = 100) String managerName,
        Integer sortOrder,
        @Size(max = 255) String description) {
}
