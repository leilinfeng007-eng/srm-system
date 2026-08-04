package com.srm.system.api.request;

import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @Size(max = 100) String roleName,
        @Size(max = 255) String description,
        Long version) {
}
