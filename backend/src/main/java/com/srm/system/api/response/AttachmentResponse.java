package com.srm.system.api.response;

import java.time.LocalDateTime;

public record AttachmentResponse(
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
        Integer currentVersion,
        String createdBy,
        LocalDateTime createdAt) {
}
