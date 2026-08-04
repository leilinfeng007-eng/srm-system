package com.srm.system.domain.model;

import java.time.Instant;

public record User(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String employeeCode,
        String email,
        String phone,
        Long mainOrganizationId,
        Long mainDepartmentId,
        Long mainPositionId,
        Boolean mustChangePassword,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        String createdBy,
        String updatedBy,
        Instant updatedAt,
        Long version) {
}
