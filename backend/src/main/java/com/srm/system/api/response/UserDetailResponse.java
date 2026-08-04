package com.srm.system.api.response;

import java.time.Instant;
import java.util.List;

public record UserDetailResponse(
        Long id,
        String username,
        String displayName,
        String employeeCode,
        String email,
        String phone,
        String status,
        Long mainOrganizationId,
        Long mainDepartmentId,
        Long mainPositionId,
        String mainOrganizationName,
        String mainDepartmentName,
        String mainPositionName,
        Instant lastLoginAt,
        Boolean mustChangePassword,
        Instant createdAt,
        List<String> roles,
        List<AssignmentHistoryResponse> assignmentHistory) {
}
