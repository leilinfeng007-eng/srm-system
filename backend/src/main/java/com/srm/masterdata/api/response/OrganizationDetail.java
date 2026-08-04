package com.srm.masterdata.api.response;

public record OrganizationDetail(
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
        Long version,
        PlantInfo plant,
        WarehouseInfo warehouse) {
}
