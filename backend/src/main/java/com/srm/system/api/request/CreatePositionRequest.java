package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank @Size(max = 64) String positionCode,
        @NotBlank @Size(max = 100) String positionName,
        @NotNull Long departmentId,
        @Size(max = 500) String responsibility,
        Integer sortOrder) {
}
