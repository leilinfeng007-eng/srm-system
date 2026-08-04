package com.srm.masterdata.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 64) String orgCode,
        @NotBlank @Size(max = 100) String orgName,
        @NotBlank @Size(max = 20) String orgType,
        Long parentId,
        Integer sortOrder,
        @Size(max = 255) String description,
        @Size(max = 64) String plantCode,
        @Size(max = 100) String plantName,
        @Size(max = 50) String timezone,
        @Size(max = 100) String country,
        @Size(max = 500) String address,
        @Size(max = 64) String warehouseCode,
        @Size(max = 100) String warehouseName,
        Long plantId,
        @Size(max = 32) String warehouseType) {
}
