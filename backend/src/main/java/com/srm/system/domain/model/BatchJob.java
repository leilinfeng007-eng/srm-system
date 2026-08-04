package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record BatchJob(
        Long id, String jobType, String objectType, String status,
        Integer totalCount, Integer successCount, Integer failCount,
        Integer progressPercent, Long attachmentId, Long resultAttachmentId,
        String idempotencyKey, LocalDateTime startedAt, LocalDateTime completedAt,
        String createdBy, LocalDateTime createdAt, Long version) {
}
