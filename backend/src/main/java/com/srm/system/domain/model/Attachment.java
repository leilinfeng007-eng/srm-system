package com.srm.system.domain.model;

import java.time.LocalDateTime;

public record Attachment(
        Long id,
        String fileName,
        String originalName,
        String mimeType,
        Long fileSize,
        String fileSha256,
        String ownerType,
        String ownerId,
        String scanStatus,
        String status,
        String createdBy,
        LocalDateTime createdAt) {
}
