package com.srm.system.api.response;

import java.time.LocalDateTime;

public record AssignmentHistoryResponse(
        String fieldName,
        String oldValue,
        String newValue,
        String changeReason,
        String changedBy,
        LocalDateTime changedAt) {
}
