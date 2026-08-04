package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record UserAssignmentHistory(
        Long id,
        Long userId,
        String fieldName,
        String oldValue,
        String newValue,
        String changeReason,
        String changedBy,
        LocalDateTime changedAt) {
}
