package com.srm.masterdata.api.response;

public record WarehouseInfo(
        Long id,
        String warehouseCode,
        String warehouseName,
        Long plantId,
        String warehouseType) {
}
