package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record Position(
        Long id,
        String positionCode,
        String positionName,
        Long departmentId,
        String category,
        String responsibility,
        Integer sortOrder,
        String status,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Long version) {
}
