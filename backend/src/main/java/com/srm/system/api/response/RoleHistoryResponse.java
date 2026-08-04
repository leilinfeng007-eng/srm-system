package com.srm.system.api.response;

import java.time.LocalDateTime;

public record RoleHistoryResponse(
        Long id,
        String action,
        String description,
        String changedBy,
        LocalDateTime changedAt) {
}
