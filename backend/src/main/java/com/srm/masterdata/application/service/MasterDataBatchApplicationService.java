package com.srm.masterdata.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.application.command.MasterDataCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MasterDataBatchApplicationService implements MasterDataBatchGateway {
    private static final Map<String, List<String>> HEADERS = Map.of(
            "PURCHASING_ORGANIZATION", List.of("poCode", "poName", "companyOrgId", "defaultCurrency"),
            "DELIVERY_LOCATION", List.of("locationCode", "locationName", "plantId", "address"),
            "CATEGORY", List.of("categoryCode", "categoryName", "parentId", "responsibleOrgId", "categoryManagerId", "riskLevel"),
            "UNIT", List.of("unitCode", "unitName"),
            "CURRENCY", List.of("currencyCode", "currencyName", "symbol", "decimalPlaces"),
            "TAX_CODE", List.of("taxCode", "taxName", "country", "taxRate"),
            "MATERIAL", List.of("materialCode", "materialName", "specification", "materialType", "baseUnit", "categoryId", "isCritical"),
            "EXTERNAL_MAPPING", List.of("objectType", "internalId", "sourceSystem", "externalId", "externalLineId", "externalVersion", "mappingStatus", "conflictSummary"));

    private final MasterDataAdministration administration;

    public MasterDataBatchApplicationService(MasterDataAdministration administration) {
        this.administration = administration;
    }

    @Override public List<String> headers(String objectType) {
        List<String> result = HEADERS.get(normalize(objectType));
        if (result == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "Unsupported batch object type");
        return result;
    }

    @Override public void importRow(String objectType, Map<String, String> v) {
        String type = normalize(objectType);
        List<String> expected = headers(type);
        for (String header : expected) {
            if (!v.containsKey(header)) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Missing column: " + header);
        }
        MasterDataCommand command = new MasterDataCommand(
                value(v,"poCode"), value(v,"poName"), number(v,"companyOrgId"), value(v,"defaultCurrency"),
                value(v,"locationCode"), value(v,"locationName"), number(v,"plantId"), value(v,"address"),
                value(v,"categoryCode"), value(v,"categoryName"), number(v,"parentId"), number(v,"responsibleOrgId"),
                number(v,"categoryManagerId"), value(v,"riskLevel"),
                value(v,"unitCode"), value(v,"unitName"),
                value(v,"currencyCode"), value(v,"currencyName"), value(v,"symbol"), integer(v,"decimalPlaces"),
                value(v,"taxCode"), value(v,"taxName"), value(v,"country"), decimal(v,"taxRate"),
                value(v,"materialCode"), value(v,"materialName"), value(v,"specification"), value(v,"materialType"),
                value(v,"baseUnit"), number(v,"categoryId"), bool(v,"isCritical"),
                value(v,"objectType"), value(v,"internalId"), value(v,"sourceSystem"), value(v,"externalId"),
                value(v,"externalLineId"), number(v,"externalVersion"), value(v,"mappingStatus"),
                value(v,"conflictSummary"));
        validateRequired(type, v);
        administration.create(MasterDataAdministration.Resource.valueOf(type), command);
    }

    @Override public ExportPage exportPage(String objectType, int page, int pageSize) {
        String type = normalize(objectType);
        headers(type);
        return administration.exportPage(MasterDataAdministration.Resource.valueOf(type), page, pageSize);
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private void validateRequired(String type, Map<String,String> values) {
        List<String> required = switch (type) {
            case "PURCHASING_ORGANIZATION" -> List.of("poCode","poName","companyOrgId");
            case "DELIVERY_LOCATION" -> List.of("locationCode","locationName","plantId");
            case "CATEGORY" -> List.of("categoryCode","categoryName","responsibleOrgId");
            case "UNIT" -> List.of("unitCode","unitName");
            case "CURRENCY" -> List.of("currencyCode","currencyName");
            case "TAX_CODE" -> List.of("taxCode","taxName","country");
            case "MATERIAL" -> List.of("materialCode","materialName","baseUnit","categoryId");
            case "EXTERNAL_MAPPING" -> List.of("objectType","internalId","sourceSystem","externalId");
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        };
        for (String key : required) if (value(values,key) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Required value is blank: " + key);
        }
    }

    private String value(Map<String,String> values,String key) {
        String value=values.get(key); return value==null||value.isBlank()?null:value.trim();
    }
    private Long number(Map<String,String> values,String key) {
        String value=value(values,key); if(value==null)return null;
        try{return Long.valueOf(value);}catch(NumberFormatException e){throw invalid(key);}
    }
    private Integer integer(Map<String,String> values,String key) {
        String value=value(values,key); if(value==null)return null;
        try{return Integer.valueOf(value);}catch(NumberFormatException e){throw invalid(key);}
    }
    private BigDecimal decimal(Map<String,String> values,String key) {
        String value=value(values,key); if(value==null)return null;
        try{return new BigDecimal(value);}catch(NumberFormatException e){throw invalid(key);}
    }
    private Boolean bool(Map<String,String> values,String key) {
        String value=value(values,key); if(value==null)return null;
        if("true".equalsIgnoreCase(value)||"1".equals(value))return true;
        if("false".equalsIgnoreCase(value)||"0".equals(value))return false;
        throw invalid(key);
    }
    private BusinessException invalid(String key){return new BusinessException(ErrorCode.VALIDATION_ERROR,"Invalid value: "+key);}
}
