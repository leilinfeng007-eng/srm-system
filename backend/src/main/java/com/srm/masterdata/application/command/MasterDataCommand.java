package com.srm.masterdata.application.command;

import java.math.BigDecimal;

public record MasterDataCommand(
        String poCode, String poName, Long companyOrgId, String defaultCurrency,
        String locationCode, String locationName, Long plantId, String address,
        String categoryCode, String categoryName, Long parentId, Long responsibleOrgId,
        Long categoryManagerId, String riskLevel,
        String unitCode, String unitName,
        String currencyCode, String currencyName, String symbol, Integer decimalPlaces,
        String taxCode, String taxName, String country, BigDecimal taxRate,
        String materialCode, String materialName, String specification, String materialType,
        String baseUnit, Long categoryId, Boolean isCritical,
        String objectType, String internalId, String sourceSystem, String externalId,
        String externalLineId, Long externalVersion, String mappingStatus,
        String conflictSummary) {
}
