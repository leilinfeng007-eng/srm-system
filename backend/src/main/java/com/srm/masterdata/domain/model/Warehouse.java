package com.srm.masterdata.domain.model;

import java.time.LocalDateTime;

public record Warehouse(
        Long id,
        Long organizationId,
        String warehouseCode,
        String warehouseName,
        Long plantId,
        String warehouseType,
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
