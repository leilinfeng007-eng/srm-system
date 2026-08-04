package com.srm.masterdata.domain.model;

import java.time.LocalDateTime;

public record Organization(
        Long id,
        String orgCode,
        String orgName,
        String orgType,
        Long parentId,
        Integer sortOrder,
        String path,
        Integer level,
        String status,
        String description,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Long version) {
}
