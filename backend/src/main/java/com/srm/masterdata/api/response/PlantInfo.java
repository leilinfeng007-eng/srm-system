package com.srm.masterdata.api.response;

public record PlantInfo(
        Long id,
        String plantCode,
        String plantName,
        String timezone,
        String country,
        String address) {
}
