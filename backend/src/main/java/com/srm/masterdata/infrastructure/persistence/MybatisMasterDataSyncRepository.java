package com.srm.masterdata.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.domain.repository.MasterDataSyncRepository;
import com.srm.masterdata.infrastructure.persistence.entity.MdCategoryEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdCurrencyEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdDeliveryLocationEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdMaterialEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdPurchasingOrganizationEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdTaxCodeEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdUnitEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdCategoryMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdCurrencyMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdDeliveryLocationMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdMaterialMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdPurchasingOrganizationMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdTaxCodeMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdUnitMapper;
import com.srm.platform.integration.InboxEventPayload;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisMasterDataSyncRepository implements MasterDataSyncRepository {

    private final ObjectMapper objectMapper;
    private final MdPurchasingOrganizationMapper poMapper;
    private final MdDeliveryLocationMapper deliveryMapper;
    private final MdCategoryMapper categoryMapper;
    private final MdUnitMapper unitMapper;
    private final MdCurrencyMapper currencyMapper;
    private final MdTaxCodeMapper taxMapper;
    private final MdMaterialMapper materialMapper;

    public MybatisMasterDataSyncRepository(ObjectMapper objectMapper,
                                           MdPurchasingOrganizationMapper poMapper,
                                           MdDeliveryLocationMapper deliveryMapper,
                                           MdCategoryMapper categoryMapper,
                                           MdUnitMapper unitMapper,
                                           MdCurrencyMapper currencyMapper,
                                           MdTaxCodeMapper taxMapper,
                                           MdMaterialMapper materialMapper) {
        this.objectMapper = objectMapper;
        this.poMapper = poMapper;
        this.deliveryMapper = deliveryMapper;
        this.categoryMapper = categoryMapper;
        this.unitMapper = unitMapper;
        this.currencyMapper = currencyMapper;
        this.taxMapper = taxMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    public void apply(InboxEventPayload event) {
        JsonNode payload = parsePayload(event.payload());
        switch (event.objectType()) {
            case "PURCHASING_ORGANIZATION" -> applyPurchasingOrganization(event, payload);
            case "DELIVERY_LOCATION" -> applyDeliveryLocation(event, payload);
            case "CATEGORY" -> applyCategory(event, payload);
            case "UNIT" -> applyUnit(event, payload);
            case "CURRENCY" -> applyCurrency(event, payload);
            case "TAX_CODE" -> applyTaxCode(event, payload);
            case "MATERIAL" -> applyMaterial(event, payload);
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Unsupported master-data object type: " + event.objectType());
        }
    }

    private void applyPurchasingOrganization(InboxEventPayload event, JsonNode payload) {
        MdPurchasingOrganizationEntity entity = poMapper.selectOne(
                new LambdaQueryWrapper<MdPurchasingOrganizationEntity>()
                        .eq(MdPurchasingOrganizationEntity::getSourceSystem, event.sourceSystem())
                        .eq(MdPurchasingOrganizationEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdPurchasingOrganizationEntity();
        entity.setPoCode(requiredText(payload, "poCode"));
        entity.setPoName(requiredText(payload, "poName"));
        entity.setCompanyOrgId(requiredLong(payload, "companyOrgId"));
        entity.setResponsibleUserId(optionalLong(payload, "responsibleUserId"));
        entity.setDefaultCurrency(text(payload, "defaultCurrency", "CNY"));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) poMapper.insert(entity); else poMapper.updateById(entity);
    }

    private void applyDeliveryLocation(InboxEventPayload event, JsonNode payload) {
        MdDeliveryLocationEntity entity = deliveryMapper.selectOne(
                new LambdaQueryWrapper<MdDeliveryLocationEntity>()
                        .eq(MdDeliveryLocationEntity::getSourceSystem, event.sourceSystem())
                        .eq(MdDeliveryLocationEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdDeliveryLocationEntity();
        entity.setLocationCode(requiredText(payload, "locationCode"));
        entity.setLocationName(requiredText(payload, "locationName"));
        entity.setPlantId(requiredLong(payload, "plantId"));
        entity.setWarehouseId(optionalLong(payload, "warehouseId"));
        entity.setAddress(optionalText(payload, "address"));
        entity.setContactPerson(optionalText(payload, "contactPerson"));
        entity.setContactPhone(optionalText(payload, "contactPhone"));
        entity.setAppointmentRequired(payload.path("appointmentRequired").asBoolean(false));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) deliveryMapper.insert(entity); else deliveryMapper.updateById(entity);
    }

    private void applyCategory(InboxEventPayload event, JsonNode payload) {
        MdCategoryEntity entity = categoryMapper.selectOne(new LambdaQueryWrapper<MdCategoryEntity>()
                .eq(MdCategoryEntity::getSourceSystem, event.sourceSystem())
                .eq(MdCategoryEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdCategoryEntity();
        entity.setCategoryCode(requiredText(payload, "categoryCode"));
        entity.setCategoryName(requiredText(payload, "categoryName"));
        entity.setParentId(optionalLong(payload, "parentId"));
        entity.setLevel(payload.path("level").asInt(0));
        entity.setResponsibleOrgId(requiredLong(payload, "responsibleOrgId"));
        entity.setCategoryManagerId(optionalLong(payload, "categoryManagerId"));
        entity.setRiskLevel(text(payload, "riskLevel", "LOW"));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) categoryMapper.insert(entity); else categoryMapper.updateById(entity);
    }

    private void applyUnit(InboxEventPayload event, JsonNode payload) {
        MdUnitEntity entity = unitMapper.selectOne(new LambdaQueryWrapper<MdUnitEntity>()
                .eq(MdUnitEntity::getSourceSystem, event.sourceSystem())
                .eq(MdUnitEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdUnitEntity();
        entity.setUnitCode(requiredText(payload, "unitCode"));
        entity.setUnitName(requiredText(payload, "unitName"));
        entity.setDimension(optionalText(payload, "dimension"));
        entity.setDecimalPlaces(payload.path("decimalPlaces").asInt(0));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) unitMapper.insert(entity); else unitMapper.updateById(entity);
    }

    private void applyCurrency(InboxEventPayload event, JsonNode payload) {
        MdCurrencyEntity entity = currencyMapper.selectOne(new LambdaQueryWrapper<MdCurrencyEntity>()
                .eq(MdCurrencyEntity::getSourceSystem, event.sourceSystem())
                .eq(MdCurrencyEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdCurrencyEntity();
        entity.setCurrencyCode(requiredText(payload, "currencyCode"));
        entity.setCurrencyName(requiredText(payload, "currencyName"));
        entity.setSymbol(optionalText(payload, "symbol"));
        entity.setDecimalPlaces(payload.path("decimalPlaces").asInt(2));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) currencyMapper.insert(entity); else currencyMapper.updateById(entity);
    }

    private void applyTaxCode(InboxEventPayload event, JsonNode payload) {
        MdTaxCodeEntity entity = taxMapper.selectOne(new LambdaQueryWrapper<MdTaxCodeEntity>()
                .eq(MdTaxCodeEntity::getSourceSystem, event.sourceSystem())
                .eq(MdTaxCodeEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdTaxCodeEntity();
        entity.setTaxCode(requiredText(payload, "taxCode"));
        entity.setTaxName(requiredText(payload, "taxName"));
        entity.setCountry(requiredText(payload, "country"));
        entity.setTaxRate(new BigDecimal(requiredText(payload, "taxRate")));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) taxMapper.insert(entity); else taxMapper.updateById(entity);
    }

    private void applyMaterial(InboxEventPayload event, JsonNode payload) {
        MdMaterialEntity entity = materialMapper.selectOne(new LambdaQueryWrapper<MdMaterialEntity>()
                .eq(MdMaterialEntity::getSourceSystem, event.sourceSystem())
                .eq(MdMaterialEntity::getExternalId, event.objectId()));
        boolean created = entity == null;
        if (created) entity = new MdMaterialEntity();
        entity.setMaterialCode(requiredText(payload, "materialCode"));
        entity.setMaterialName(requiredText(payload, "materialName"));
        entity.setSpecification(optionalText(payload, "specification"));
        entity.setMaterialType(requiredText(payload, "materialType"));
        entity.setBaseUnit(requiredText(payload, "baseUnit"));
        entity.setCategoryId(optionalLong(payload, "categoryId"));
        entity.setMaterialVersion(text(payload, "materialVersion", String.valueOf(event.objectVersion())));
        entity.setIsCritical(payload.path("isCritical").asBoolean(false));
        entity.setStatus(text(payload, "status", "ACTIVE"));
        stamp(entity, event, created);
        if (created) materialMapper.insert(entity); else materialMapper.updateById(entity);
    }

    private JsonNode parsePayload(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node == null || !node.isObject()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Inbox payload must be a JSON object");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Inbox payload is not valid JSON");
        }
    }

    private String requiredText(JsonNode payload, String field) {
        String value = optionalText(payload, field);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Missing required Inbox field: " + field);
        }
        return value;
    }

    private Long requiredLong(JsonNode payload, String field) {
        Long value = optionalLong(payload, field);
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Missing required Inbox field: " + field);
        }
        return value;
    }

    private String text(JsonNode payload, String field, String defaultValue) {
        String value = optionalText(payload, field);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String optionalText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long optionalLong(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private void stamp(MdPurchasingOrganizationEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdDeliveryLocationEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdCategoryEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdUnitEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdCurrencyEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdTaxCodeEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private void stamp(MdMaterialEntity entity, InboxEventPayload event, boolean created) {
        entity.setSourceType("EXTERNAL"); entity.setSourceSystem(event.sourceSystem());
        entity.setExternalId(event.objectId()); entity.setExternalVersion(event.objectVersion());
        entity.setLastSyncAt(LocalDateTime.now()); entity.setUpdatedBy(actor(event));
        if (created) entity.setCreatedBy(actor(event));
    }

    private String actor(InboxEventPayload event) {
        return "integration:" + event.sourceSystem();
    }
}
