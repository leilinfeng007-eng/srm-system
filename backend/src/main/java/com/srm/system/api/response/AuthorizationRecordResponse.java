package com.srm.system.api.response;

import java.time.LocalDateTime;

public record AuthorizationRecordResponse(
        LocalDateTime occurredAt,
        String actionCode,
        String actionLabel,
        Long roleId,
        String roleName,
        String beforeSummary,
        String afterSummary,
        String resultCode,
        String changedBy) {
}
