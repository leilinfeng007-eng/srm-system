package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 64) String roleCode,
        @NotBlank @Size(max = 100) String roleName,
        @Size(max = 255) String description,
        @Size(max = 32) String roleCategory) {
}
