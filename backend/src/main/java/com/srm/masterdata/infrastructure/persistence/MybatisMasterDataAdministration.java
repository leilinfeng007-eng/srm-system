package com.srm.masterdata.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.application.command.MasterDataCommand;
import com.srm.masterdata.application.service.MasterDataAdministration;
import com.srm.masterdata.application.service.MasterDataBatchGateway;
import com.srm.masterdata.infrastructure.persistence.entity.*;
import com.srm.masterdata.infrastructure.persistence.mapper.*;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.service.AuditRecorder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisMasterDataAdministration implements MasterDataAdministration {
    private final DataScopeAuthorizationService scopes;
    private final MdPurchasingOrganizationMapper po;
    private final MdDeliveryLocationMapper locations;
    private final MdCategoryMapper categories;
    private final MdUnitMapper units;
    private final MdCurrencyMapper currencies;
    private final MdTaxCodeMapper taxes;
    private final MdMaterialMapper materials;
    private final MdExternalMappingMapper mappings;
    private final MdPlantMapper plants;
    private final AuditRecorder audit;

    public MybatisMasterDataAdministration(DataScopeAuthorizationService scopes,
            MdPurchasingOrganizationMapper po, MdDeliveryLocationMapper locations,
            MdCategoryMapper categories, MdUnitMapper units, MdCurrencyMapper currencies,
            MdTaxCodeMapper taxes, MdMaterialMapper materials,
            MdExternalMappingMapper mappings, MdPlantMapper plants, AuditRecorder audit) {
        this.scopes = scopes; this.po = po; this.locations = locations;
        this.categories = categories; this.units = units; this.currencies = currencies;
        this.taxes = taxes; this.materials = materials; this.mappings = mappings;
        this.plants = plants;
        this.audit = audit;
    }

    @Override
    public PageResult<?> list(Resource resource, int page, int pageSize, String filter) {
        return list(resource, page, pageSize,
                resource == Resource.EXTERNAL_MAPPING ? null : filter,
                null, resource == Resource.EXTERNAL_MAPPING ? filter : null);
    }

    @Override
    public PageResult<?> list(Resource resource, int page, int pageSize, String keyword,
                              String status, String sourceSystem) {
        if (page < 1 || pageSize < 1 || pageSize > 200) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid paging parameters");
        }
        return switch (resource) {
            case PURCHASING_ORGANIZATION -> listPurchasingOrganizations(page, pageSize, keyword, status);
            case DELIVERY_LOCATION -> listDeliveryLocations(page, pageSize, keyword, status);
            case CATEGORY -> listCategories(page, pageSize, keyword, status);
            case UNIT -> listUnits(page, pageSize, keyword, status);
            case CURRENCY -> listCurrencies(page, pageSize, keyword, status);
            case TAX_CODE -> listTaxCodes(page, pageSize, keyword, status);
            case MATERIAL -> listMaterials(page, pageSize, keyword, status);
            case EXTERNAL_MAPPING -> listMappings(page, pageSize, sourceSystem, status);
        };
    }

    @Override
    public Object get(Resource resource, Long id) {
        if (id == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return switch (resource) {
            case PURCHASING_ORGANIZATION -> {
                var entity = required(po.selectById(id));
                requireOrganization(read("masterdata:purchasing-organization:view", "PURCHASING_ORGANIZATION"), entity.getCompanyOrgId());
                yield entity;
            }
            case DELIVERY_LOCATION -> {
                var entity = required(locations.selectById(id));
                requirePlant(read("masterdata:delivery-location:view", "PLANT"), entity.getPlantId());
                yield entity;
            }
            case CATEGORY -> {
                var entity = required(categories.selectById(id));
                requireCategory(read("masterdata:category:view", "CATEGORY"), entity);
                yield entity;
            }
            case UNIT -> { requireAll(read("masterdata:unit:view", "ORGANIZATION")); yield required(units.selectById(id)); }
            case CURRENCY -> { requireAll(read("masterdata:currency:view", "ORGANIZATION")); yield required(currencies.selectById(id)); }
            case TAX_CODE -> { requireAll(read("masterdata:tax-code:view", "ORGANIZATION")); yield required(taxes.selectById(id)); }
            case MATERIAL -> {
                var entity = required(materials.selectById(id));
                requireCategory(read("masterdata:material:view", "CATEGORY"), categories.selectById(entity.getCategoryId()));
                yield entity;
            }
            case EXTERNAL_MAPPING -> { requireAll(read("masterdata:external-mapping:view", "ORGANIZATION")); yield required(mappings.selectById(id)); }
        };
    }

    @Override
    @Transactional
    public Object create(Resource resource, MasterDataCommand c) {
        if (c == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request body is required");
        Object result = switch (resource) {
            case PURCHASING_ORGANIZATION -> createPurchasingOrganization(c);
            case DELIVERY_LOCATION -> createDeliveryLocation(c);
            case CATEGORY -> createCategory(c);
            case UNIT -> createUnit(c);
            case CURRENCY -> createCurrency(c);
            case TAX_CODE -> createTaxCode(c);
            case MATERIAL -> createMaterial(c);
            case EXTERNAL_MAPPING -> createMapping(c);
        };
        audit.record("MASTER_DATA_CREATED", resource.name(), entityId(resource, result),
                "SUCCESS", null, "resource=" + resource.name(), null);
        return result;
    }

    @Override
    @Transactional
    public void update(Resource resource, Long id, MasterDataCommand c) {
        if (c == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Request body is required");
        switch (resource) {
            case PURCHASING_ORGANIZATION -> {
                var e=required(po.selectById(id)); requireOrganization(write("masterdata:purchasing-organization:update","PURCHASING_ORGANIZATION"),e.getCompanyOrgId());
                if(c.poName()!=null)e.setPoName(c.poName()); if(c.defaultCurrency()!=null)e.setDefaultCurrency(c.defaultCurrency()); e.setUpdatedBy(actor()); po.updateById(e);
            }
            case DELIVERY_LOCATION -> {
                var e=required(locations.selectById(id)); requirePlant(write("masterdata:delivery-location:update","PLANT"),e.getPlantId());
                if(c.locationName()!=null)e.setLocationName(c.locationName()); if(c.address()!=null)e.setAddress(c.address()); e.setUpdatedBy(actor()); locations.updateById(e);
            }
            case CATEGORY -> {
                var e=required(categories.selectById(id)); requireCategory(write("masterdata:category:update","CATEGORY"),e);
                if(c.categoryName()!=null)e.setCategoryName(c.categoryName()); if(c.riskLevel()!=null)e.setRiskLevel(c.riskLevel()); e.setUpdatedBy(actor()); categories.updateById(e);
            }
            case UNIT -> { requireAll(write("masterdata:unit:update","ORGANIZATION")); var e=required(units.selectById(id)); if(c.unitName()!=null)e.setUnitName(c.unitName()); e.setUpdatedBy(actor()); units.updateById(e); }
            case CURRENCY -> { requireAll(write("masterdata:currency:update","ORGANIZATION")); var e=required(currencies.selectById(id)); if(c.currencyName()!=null)e.setCurrencyName(c.currencyName()); e.setUpdatedBy(actor()); currencies.updateById(e); }
            case TAX_CODE -> { requireAll(write("masterdata:tax-code:update","ORGANIZATION")); var e=required(taxes.selectById(id)); if(c.taxName()!=null)e.setTaxName(c.taxName()); if(c.taxRate()!=null)e.setTaxRate(c.taxRate()); e.setUpdatedBy(actor()); taxes.updateById(e); }
            case MATERIAL -> {
                var e=required(materials.selectById(id)); requireCategory(write("masterdata:material:update","CATEGORY"),categories.selectById(e.getCategoryId()));
                if(c.materialName()!=null)e.setMaterialName(c.materialName()); if(c.specification()!=null)e.setSpecification(c.specification()); if(c.isCritical()!=null)e.setIsCritical(c.isCritical()); e.setUpdatedBy(actor()); materials.updateById(e);
            }
            case EXTERNAL_MAPPING -> throw new BusinessException(ErrorCode.CONFLICT, "External mappings are immutable; create a new version");
        }
        audit.record("MASTER_DATA_UPDATED", resource.name(), String.valueOf(id),
                "SUCCESS", "resource=" + resource.name(), "resource=" + resource.name(), null);
    }

    @Override
    @Transactional
    public void setStatus(Resource resource, Long id, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid status");
        }
        switch (resource) {
            case PURCHASING_ORGANIZATION -> { var e=required(po.selectById(id)); requireOrganization(write("masterdata:purchasing-organization:"+action(status),"PURCHASING_ORGANIZATION"),e.getCompanyOrgId()); e.setStatus(status);e.setUpdatedBy(actor());po.updateById(e); }
            case DELIVERY_LOCATION -> { var e=required(locations.selectById(id)); requirePlant(write("masterdata:delivery-location:"+action(status),"PLANT"),e.getPlantId()); e.setStatus(status);e.setUpdatedBy(actor());locations.updateById(e); }
            case CATEGORY -> { var e=required(categories.selectById(id)); requireCategory(write("masterdata:category:"+action(status),"CATEGORY"),e);e.setStatus(status);e.setUpdatedBy(actor());categories.updateById(e); }
            case UNIT -> { requireAll(write("masterdata:unit:"+action(status),"ORGANIZATION")); var e=required(units.selectById(id));e.setStatus(status);e.setUpdatedBy(actor());units.updateById(e); }
            case CURRENCY -> { requireAll(write("masterdata:currency:"+action(status),"ORGANIZATION")); var e=required(currencies.selectById(id));e.setStatus(status);e.setUpdatedBy(actor());currencies.updateById(e); }
            case TAX_CODE -> { requireAll(write("masterdata:tax-code:"+action(status),"ORGANIZATION")); var e=required(taxes.selectById(id));e.setStatus(status);e.setUpdatedBy(actor());taxes.updateById(e); }
            case MATERIAL -> { var e=required(materials.selectById(id));requireCategory(write("masterdata:material:"+action(status),"CATEGORY"),categories.selectById(e.getCategoryId()));e.setStatus(status);e.setUpdatedBy(actor());materials.updateById(e); }
            case EXTERNAL_MAPPING -> throw new BusinessException(ErrorCode.CONFLICT, "External mappings do not support status changes");
        }
        audit.record("MASTER_DATA_STATUS_CHANGED", resource.name(), String.valueOf(id),
                "SUCCESS", null, "status=" + status, null);
    }

    @Override
    public MasterDataBatchGateway.ExportPage exportPage(Resource resource, int page, int pageSize) {
        PageResult<?> result = list(resource, page, pageSize, null);
        List<String> headers = switch (resource) {
            case PURCHASING_ORGANIZATION -> List.of("poCode","poName","companyOrgId","defaultCurrency");
            case DELIVERY_LOCATION -> List.of("locationCode","locationName","plantId","address");
            case CATEGORY -> List.of("categoryCode","categoryName","parentId","responsibleOrgId","categoryManagerId","riskLevel");
            case UNIT -> List.of("unitCode","unitName");
            case CURRENCY -> List.of("currencyCode","currencyName","symbol","decimalPlaces");
            case TAX_CODE -> List.of("taxCode","taxName","country","taxRate");
            case MATERIAL -> List.of("materialCode","materialName","specification","materialType","baseUnit","categoryId","isCritical");
            case EXTERNAL_MAPPING -> List.of("objectType","internalId","sourceSystem","externalId","externalLineId","externalVersion","mappingStatus","conflictSummary");
        };
        List<List<String>> rows = result.items().stream().map(entity -> exportRow(resource, entity)).toList();
        return new MasterDataBatchGateway.ExportPage(headers, rows, result.total());
    }

    private List<String> exportRow(Resource resource, Object value) {
        return switch (resource) {
            case PURCHASING_ORGANIZATION -> { var e=(MdPurchasingOrganizationEntity)value; yield values(e.getPoCode(),e.getPoName(),e.getCompanyOrgId(),e.getDefaultCurrency()); }
            case DELIVERY_LOCATION -> { var e=(MdDeliveryLocationEntity)value; yield values(e.getLocationCode(),e.getLocationName(),e.getPlantId(),e.getAddress()); }
            case CATEGORY -> { var e=(MdCategoryEntity)value; yield values(e.getCategoryCode(),e.getCategoryName(),e.getParentId(),e.getResponsibleOrgId(),e.getCategoryManagerId(),e.getRiskLevel()); }
            case UNIT -> { var e=(MdUnitEntity)value; yield values(e.getUnitCode(),e.getUnitName()); }
            case CURRENCY -> { var e=(MdCurrencyEntity)value; yield values(e.getCurrencyCode(),e.getCurrencyName(),e.getSymbol(),e.getDecimalPlaces()); }
            case TAX_CODE -> { var e=(MdTaxCodeEntity)value; yield values(e.getTaxCode(),e.getTaxName(),e.getCountry(),e.getTaxRate()); }
            case MATERIAL -> { var e=(MdMaterialEntity)value; yield values(e.getMaterialCode(),e.getMaterialName(),e.getSpecification(),e.getMaterialType(),e.getBaseUnit(),e.getCategoryId(),e.getIsCritical()); }
            case EXTERNAL_MAPPING -> { var e=(MdExternalMappingEntity)value; yield values(e.getObjectType(),e.getInternalId(),e.getSourceSystem(),e.getExternalId(),e.getExternalLineId(),e.getExternalVersion(),e.getMappingStatus(),e.getConflictSummary()); }
        };
    }

    private List<String> values(Object... values) {
        return java.util.Arrays.stream(values).map(v -> v == null ? "" : String.valueOf(v)).toList();
    }

    private String entityId(Resource resource,Object value){return String.valueOf(switch(resource){case PURCHASING_ORGANIZATION->((MdPurchasingOrganizationEntity)value).getId();case DELIVERY_LOCATION->((MdDeliveryLocationEntity)value).getId();case CATEGORY->((MdCategoryEntity)value).getId();case UNIT->((MdUnitEntity)value).getId();case CURRENCY->((MdCurrencyEntity)value).getId();case TAX_CODE->((MdTaxCodeEntity)value).getId();case MATERIAL->((MdMaterialEntity)value).getId();case EXTERNAL_MAPPING->((MdExternalMappingEntity)value).getId();});}

    private PageResult<?> listPurchasingOrganizations(int page,int size,String keyword,String status){var s=read("masterdata:purchasing-organization:view","PURCHASING_ORGANIZATION");var q=new LambdaQueryWrapper<MdPurchasingOrganizationEntity>();if(!s.isAllScope()&&s.allowedOrgIds().isEmpty())q.apply("1 = 0");else if(!s.isAllScope())q.in(MdPurchasingOrganizationEntity::getCompanyOrgId,s.allowedOrgIds());if(has(keyword))q.and(x->x.like(MdPurchasingOrganizationEntity::getPoCode,keyword).or().like(MdPurchasingOrganizationEntity::getPoName,keyword));if(has(status))q.eq(MdPurchasingOrganizationEntity::getStatus,status);return page(po,q,page,size);}
    private PageResult<?> listDeliveryLocations(int page,int size,String keyword,String status){var s=read("masterdata:delivery-location:view","PLANT");var q=new LambdaQueryWrapper<MdDeliveryLocationEntity>();if(!s.isAllScope())q.in(MdDeliveryLocationEntity::getPlantId,allowedPlantIds(s));if(has(keyword))q.and(x->x.like(MdDeliveryLocationEntity::getLocationCode,keyword).or().like(MdDeliveryLocationEntity::getLocationName,keyword).or().like(MdDeliveryLocationEntity::getAddress,keyword));if(has(status))q.eq(MdDeliveryLocationEntity::getStatus,status);return page(locations,q,page,size);}
    private PageResult<?> listCategories(int page,int size,String keyword,String status){var q=new LambdaQueryWrapper<MdCategoryEntity>();applyCategoryScope(q,read("masterdata:category:view","CATEGORY"));if(has(keyword))q.and(x->x.like(MdCategoryEntity::getCategoryCode,keyword).or().like(MdCategoryEntity::getCategoryName,keyword));if(has(status))q.eq(MdCategoryEntity::getStatus,status);return page(categories,q,page,size);}
    private PageResult<?> listUnits(int page,int size,String keyword,String status){requireAll(read("masterdata:unit:view","ORGANIZATION"));var q=new LambdaQueryWrapper<MdUnitEntity>();if(has(keyword))q.and(x->x.like(MdUnitEntity::getUnitCode,keyword).or().like(MdUnitEntity::getUnitName,keyword));if(has(status))q.eq(MdUnitEntity::getStatus,status);return page(units,q,page,size);}
    private PageResult<?> listCurrencies(int page,int size,String keyword,String status){requireAll(read("masterdata:currency:view","ORGANIZATION"));var q=new LambdaQueryWrapper<MdCurrencyEntity>();if(has(keyword))q.and(x->x.like(MdCurrencyEntity::getCurrencyCode,keyword).or().like(MdCurrencyEntity::getCurrencyName,keyword));if(has(status))q.eq(MdCurrencyEntity::getStatus,status);return page(currencies,q,page,size);}
    private PageResult<?> listTaxCodes(int page,int size,String keyword,String status){requireAll(read("masterdata:tax-code:view","ORGANIZATION"));var q=new LambdaQueryWrapper<MdTaxCodeEntity>();if(has(keyword))q.and(x->x.like(MdTaxCodeEntity::getTaxCode,keyword).or().like(MdTaxCodeEntity::getTaxName,keyword));if(has(status))q.eq(MdTaxCodeEntity::getStatus,status);return page(taxes,q,page,size);}
    private PageResult<?> listMaterials(int page,int size,String keyword,String status){var q=new LambdaQueryWrapper<MdMaterialEntity>();applyMaterialScope(q,read("masterdata:material:view","CATEGORY"));if(has(keyword))q.and(x->x.like(MdMaterialEntity::getMaterialCode,keyword).or().like(MdMaterialEntity::getMaterialName,keyword));if(has(status))q.eq(MdMaterialEntity::getStatus,status);return page(materials,q,page,size);}
    private PageResult<?> listMappings(int page,int size,String source,String status){requireAll(read("masterdata:external-mapping:view","ORGANIZATION"));var q=new LambdaQueryWrapper<MdExternalMappingEntity>();if(has(source))q.eq(MdExternalMappingEntity::getSourceSystem,source);if(has(status))q.eq(MdExternalMappingEntity::getMappingStatus,status);return page(mappings,q,page,size);}

    private Object createPurchasingOrganization(MasterDataCommand c){requireOrganization(write("masterdata:purchasing-organization:create","PURCHASING_ORGANIZATION"),c.companyOrgId());unique(po,new LambdaQueryWrapper<MdPurchasingOrganizationEntity>().eq(MdPurchasingOrganizationEntity::getPoCode,c.poCode()));var e=new MdPurchasingOrganizationEntity();e.setPoCode(c.poCode());e.setPoName(c.poName());e.setCompanyOrgId(c.companyOrgId());e.setDefaultCurrency(c.defaultCurrency()==null?"CNY":c.defaultCurrency());stamp(e);po.insert(e);return e;}
    private Object createDeliveryLocation(MasterDataCommand c){requirePlant(write("masterdata:delivery-location:create","PLANT"),c.plantId());unique(locations,new LambdaQueryWrapper<MdDeliveryLocationEntity>().eq(MdDeliveryLocationEntity::getLocationCode,c.locationCode()));var e=new MdDeliveryLocationEntity();e.setLocationCode(c.locationCode());e.setLocationName(c.locationName());e.setPlantId(c.plantId());e.setAddress(c.address());stamp(e);locations.insert(e);return e;}
    private Object createCategory(MasterDataCommand c){var target=new MdCategoryEntity();target.setResponsibleOrgId(c.responsibleOrgId());target.setCategoryManagerId(c.categoryManagerId());requireCategory(write("masterdata:category:create","CATEGORY"),target);unique(categories,new LambdaQueryWrapper<MdCategoryEntity>().eq(MdCategoryEntity::getCategoryCode,c.categoryCode()));var e=new MdCategoryEntity();e.setCategoryCode(c.categoryCode());e.setCategoryName(c.categoryName());e.setParentId(c.parentId());e.setResponsibleOrgId(c.responsibleOrgId());e.setCategoryManagerId(c.categoryManagerId());e.setLevel(0);e.setRiskLevel(c.riskLevel()==null?"LOW":c.riskLevel());stamp(e);categories.insert(e);return e;}
    private Object createUnit(MasterDataCommand c){requireAll(write("masterdata:unit:create","ORGANIZATION"));unique(units,new LambdaQueryWrapper<MdUnitEntity>().eq(MdUnitEntity::getUnitCode,c.unitCode()));var e=new MdUnitEntity();e.setUnitCode(c.unitCode());e.setUnitName(c.unitName());stamp(e);units.insert(e);return e;}
    private Object createCurrency(MasterDataCommand c){requireAll(write("masterdata:currency:create","ORGANIZATION"));unique(currencies,new LambdaQueryWrapper<MdCurrencyEntity>().eq(MdCurrencyEntity::getCurrencyCode,c.currencyCode()));var e=new MdCurrencyEntity();e.setCurrencyCode(c.currencyCode());e.setCurrencyName(c.currencyName());e.setSymbol(c.symbol());e.setDecimalPlaces(c.decimalPlaces()==null?2:c.decimalPlaces());stamp(e);currencies.insert(e);return e;}
    private Object createTaxCode(MasterDataCommand c){requireAll(write("masterdata:tax-code:create","ORGANIZATION"));unique(taxes,new LambdaQueryWrapper<MdTaxCodeEntity>().eq(MdTaxCodeEntity::getTaxCode,c.taxCode()));var e=new MdTaxCodeEntity();e.setTaxCode(c.taxCode());e.setTaxName(c.taxName());e.setCountry(c.country());e.setTaxRate(c.taxRate()==null?BigDecimal.ZERO:c.taxRate());stamp(e);taxes.insert(e);return e;}
    private Object createMaterial(MasterDataCommand c){requireCategory(write("masterdata:material:create","CATEGORY"),categories.selectById(c.categoryId()));unique(materials,new LambdaQueryWrapper<MdMaterialEntity>().eq(MdMaterialEntity::getMaterialCode,c.materialCode()));var e=new MdMaterialEntity();e.setMaterialCode(c.materialCode());e.setMaterialName(c.materialName());e.setSpecification(c.specification());e.setMaterialType(c.materialType());e.setBaseUnit(c.baseUnit());e.setCategoryId(c.categoryId());e.setIsCritical(Boolean.TRUE.equals(c.isCritical()));e.setMaterialVersion("1");stamp(e);materials.insert(e);return e;}
    private Object createMapping(MasterDataCommand c){requireAll(write("masterdata:external-mapping:create","ORGANIZATION"));var e=new MdExternalMappingEntity();e.setObjectType(c.objectType());e.setInternalId(c.internalId());e.setSourceSystem(c.sourceSystem());e.setExternalId(c.externalId());e.setExternalLineId(c.externalLineId());e.setExternalVersion(c.externalVersion());e.setMappingStatus(c.mappingStatus()==null?"ACTIVE":c.mappingStatus());e.setConflictSummary(c.conflictSummary());e.setCreatedAt(LocalDateTime.now());mappings.insert(e);return e;}

    private DataScopeResolution read(String permission,String dimension){return scopes.requireRead(permission,"masterdata",dimension);}
    private DataScopeResolution write(String permission,String dimension){return scopes.requireWrite(permission,"masterdata",dimension);}
    private void requireAll(DataScopeResolution s){if(!s.isAllScope())throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void requireOrganization(DataScopeResolution s,Long id){if(!s.isAllScope()&&!s.coversOrganization(id))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void requirePlant(DataScopeResolution s,Long id){if(s.isAllScope())return;var p=id==null?null:plants.selectById(id);if(p==null||!s.coversOrganization(p.getOrganizationId()))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private void requireCategory(DataScopeResolution s,MdCategoryEntity c){if(c==null||!(s.isAllScope()||(s.isOrgScope()&&s.coversOrganization(c.getResponsibleOrgId()))||(s.isSelfScope()&&s.coversOwner(c.getCategoryManagerId()))))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private List<Long> allowedPlantIds(DataScopeResolution s){if(s.allowedOrgIds().isEmpty())return List.of(-1L);return plants.selectList(new LambdaQueryWrapper<MdPlantEntity>().in(MdPlantEntity::getOrganizationId,s.allowedOrgIds())).stream().map(MdPlantEntity::getId).toList();}
    private void applyCategoryScope(LambdaQueryWrapper<MdCategoryEntity> q,DataScopeResolution s){if(s.isOrgScope()&&s.allowedOrgIds().isEmpty())q.apply("1 = 0");else if(s.isOrgScope())q.in(MdCategoryEntity::getResponsibleOrgId,s.allowedOrgIds());else if(s.isSelfScope())q.eq(MdCategoryEntity::getCategoryManagerId,s.ownerUserId());else if(!s.isAllScope())q.apply("1 = 0");}
    private void applyMaterialScope(LambdaQueryWrapper<MdMaterialEntity> q,DataScopeResolution s){if(s.isAllScope())return;var cq=new LambdaQueryWrapper<MdCategoryEntity>();applyCategoryScope(cq,s);var ids=categories.selectList(cq).stream().map(MdCategoryEntity::getId).toList();if(ids.isEmpty())q.apply("1 = 0");else q.in(MdMaterialEntity::getCategoryId,ids);}
    private <T> T required(T entity){if(entity==null)throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);return entity;}
    private <T> void unique(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,LambdaQueryWrapper<T> q){if(mapper.selectCount(q)>0)throw new BusinessException(ErrorCode.CONFLICT,"Code exists");}
    private <T> PageResult<T> page(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,LambdaQueryWrapper<T> q,int p,int s){long total=mapper.selectCount(q);return PageResult.of(mapper.selectList(q.last("LIMIT "+((p-1)*s)+","+s)),p,s,total);}
    private String action(String status){return "ACTIVE".equals(status)?"enable":"disable";}
    private boolean has(String value){return value!=null&&!value.isBlank();}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private void stamp(MdPurchasingOrganizationEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdDeliveryLocationEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdCategoryEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdUnitEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdCurrencyEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdTaxCodeEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
    private void stamp(MdMaterialEntity e){e.setStatus("ACTIVE");e.setSourceType("MANUAL");e.setSourceSystem("SRM");e.setCreatedBy(actor());e.setUpdatedBy(actor());}
}
