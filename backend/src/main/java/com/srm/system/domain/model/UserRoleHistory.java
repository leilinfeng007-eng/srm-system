package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record UserRoleHistory(
        Long id,
        Long userId,
        Long roleId,
        String action,
        String previousStatus,
        String newStatus,
        String changedBy,
        LocalDateTime changedAt) {
}
