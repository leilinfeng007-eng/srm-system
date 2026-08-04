package com.srm.masterdata.domain.model;

import java.time.LocalDateTime;

public record Plant(
        Long id,
        Long organizationId,
        String plantCode,
        String plantName,
        String timezone,
        String country,
        String address,
        String status,
        String sourceType,
        String sourceSystem,
        String externalId,
        Long externalVersion,
        LocalDateTime lastSyncAt,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Long version) {
}
