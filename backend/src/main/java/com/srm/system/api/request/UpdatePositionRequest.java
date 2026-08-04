package com.srm.system.api.request;

import jakarta.validation.constraints.Size;

public record UpdatePositionRequest(
        @Size(max = 100) String positionName,
        Long departmentId,
        @Size(max = 500) String responsibility,
        Integer sortOrder,
        Long version) {
}
