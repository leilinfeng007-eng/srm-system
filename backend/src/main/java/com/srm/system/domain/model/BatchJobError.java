package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record BatchJobError(
        Long id, Long jobId, Integer rowNumber, String fieldName,
        String errorCode, String errorMessage, LocalDateTime createdAt) {
}
