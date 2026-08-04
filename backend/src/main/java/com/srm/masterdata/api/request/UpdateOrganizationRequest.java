package com.srm.masterdata.api.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 100) String orgName,
        Integer sortOrder,
        @Size(max = 255) String description,
        Long version,
        @Size(max = 100) String plantName,
        @Size(max = 50) String timezone,
        @Size(max = 100) String country,
        @Size(max = 500) String address,
        @Size(max = 100) String warehouseName,
        Long plantId,
        @Size(max = 32) String warehouseType) {
}
