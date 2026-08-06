package com.srm.system.api.response;

import java.time.LocalDateTime;

public record PositionResponse(
        Long id,
        String positionCode,
        String positionName,
        Long departmentId,
        String departmentName,
        String category,
        String responsibility,
        Integer sortOrder,
        String status,
        long userCount,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
