package com.srm.system.api.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        @Size(max = 64) String employeeCode,
        @Size(max = 128) String email,
        @Size(max = 32) String phone,
        Long mainOrganizationId,
        Long mainDepartmentId,
        Long mainPositionId,
        Long version) {
}
