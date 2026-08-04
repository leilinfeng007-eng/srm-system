package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 64) String employeeCode,
        @Size(max = 128) String email,
        @Size(max = 32) String phone,
        @NotNull Long mainOrganizationId,
        @NotNull Long mainDepartmentId,
        @NotNull Long mainPositionId) {
}
