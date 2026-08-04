package com.srm.masterdata.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.api.request.CreateOrganizationRequest;
import com.srm.masterdata.api.request.UpdateOrganizationRequest;
import com.srm.masterdata.api.response.OrganizationDetail;
import com.srm.masterdata.api.response.OrganizationTreeNode;
import com.srm.masterdata.api.response.PlantInfo;
import com.srm.masterdata.api.response.WarehouseInfo;
import com.srm.masterdata.domain.model.Organization;
import com.srm.masterdata.domain.model.Plant;
import com.srm.masterdata.domain.model.Warehouse;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.masterdata.domain.repository.PlantRepository;
import com.srm.masterdata.domain.repository.WarehouseRepository;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.service.AuditRecorder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationApplicationService {

    private static final String[] VALID_PARENT_FOR_COMPANY = {"GROUP"};
    private static final String[] VALID_PARENT_FOR_BUSINESS_UNIT = {"COMPANY"};
    private static final String[] VALID_PARENT_FOR_PLANT = {"BUSINESS_UNIT"};
    private static final String[] VALID_PARENT_FOR_WAREHOUSE = {"PLANT"};

    private final OrganizationRepository organizationRepository;
    private final PlantRepository plantRepository;
    private final WarehouseRepository warehouseRepository;
    private final DataScopeAuthorizationService dataScope;
    private final AuditRecorder audit;

    public OrganizationApplicationService(OrganizationRepository organizationRepository,
                                          PlantRepository plantRepository,
                                          WarehouseRepository warehouseRepository,
                                          DataScopeAuthorizationService dataScope,
                                          AuditRecorder audit) {
        this.organizationRepository = organizationRepository;
        this.plantRepository = plantRepository;
        this.warehouseRepository = warehouseRepository;
        this.dataScope = dataScope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<OrganizationTreeNode> listOrganizations() {
        DataScopeResolution scope = dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION");
        List<Organization> all = organizationRepository.findAll().stream()
                .filter(org -> covers(scope, org.id()))
                .toList();
        Map<Long, List<Organization>> childrenMap = all.stream()
                .filter(o -> o.parentId() != null)
                .collect(Collectors.groupingBy(Organization::parentId));
        return all.stream()
                .filter(o -> o.parentId() == null)
                .map(o -> toTreeNode(o, childrenMap))
                .sorted(Comparator.comparing(OrganizationTreeNode::sortOrder))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDetail getOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Organization not found"));
        requireCovers(dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION"), id);
        return toDetail(org);
    }

    @Transactional
    public OrganizationDetail createOrganization(CreateOrganizationRequest req) {
        DataScopeResolution scope = dataScope.requireWrite(
                "masterdata:organization:create", "masterdata", "ORGANIZATION");
        if (!scope.isAllScope() && (req.parentId() == null || !scope.coversOrganization(req.parentId()))) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        String actor = currentUsername();
        if (organizationRepository.existsByOrgCode(req.orgCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Organization code already exists");
        }
        String orgType = req.orgType().toUpperCase();
        validateParentType(orgType, req.parentId());

        int level = 0;
        String path = null;
        if (req.parentId() != null) {
            Organization parent = organizationRepository.findById(req.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent organization not found"));
            level = parent.level() + 1;
            path = (parent.path() != null ? parent.path() + "/" : "/") + parent.id();
        }

        Organization org = new Organization(null, req.orgCode(), req.orgName(), orgType,
                req.parentId(), req.sortOrder() != null ? req.sortOrder() : 0, path, level,
                "ACTIVE", req.description(), actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
        Organization saved = organizationRepository.save(org);

        if ("PLANT".equals(orgType)) {
            Plant plant = new Plant(null, saved.id(), req.plantCode() != null ? req.plantCode() : req.orgCode(),
                    req.plantName() != null ? req.plantName() : req.orgName(),
                    req.timezone() != null ? req.timezone() : "UTC", req.country(), req.address(),
                    "ACTIVE", "MANUAL", "SRM", null, 0L, null,
                    actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
            plantRepository.save(plant);
        } else if ("WAREHOUSE".equals(orgType)) {
            Long plantId = req.plantId();
            if (plantId == null) {
                plantId = plantRepository.findByOrganizationId(req.parentId())
                        .map(Plant::id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                                "Parent PLANT organization has no plant extension"));
            } else {
                Long parentPlantId = plantRepository.findByOrganizationId(req.parentId())
                        .map(Plant::id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                                "Parent PLANT organization has no plant extension"));
                if (!plantId.equals(parentPlantId)) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "Warehouse plant must match its parent PLANT organization");
                }
            }
            Warehouse warehouse = new Warehouse(null, saved.id(),
                    req.warehouseCode() != null ? req.warehouseCode() : req.orgCode(),
                    req.warehouseName() != null ? req.warehouseName() : req.orgName(),
                    plantId,
                    req.warehouseType(), "ACTIVE", "MANUAL", "SRM", null, 0L, null,
                    actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
            warehouseRepository.save(warehouse);
        }

        audit.record("ORGANIZATION_CREATED", "ORGANIZATION", String.valueOf(saved.id()),
                "SUCCESS", null, summary(saved), null);
        return toDetail(saved);
    }

    @Transactional
    public OrganizationDetail updateOrganization(Long id, UpdateOrganizationRequest req) {
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Organization not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:update", "masterdata", "ORGANIZATION"), id);
        Organization updated = new Organization(
                existing.id(), existing.orgCode(),
                req.orgName() != null ? req.orgName() : existing.orgName(),
                existing.orgType(), existing.parentId(),
                req.sortOrder() != null ? req.sortOrder() : existing.sortOrder(),
                existing.path(), existing.level(), existing.status(),
                req.description() != null ? req.description() : existing.description(),
                existing.createdBy(), existing.createdAt(), currentUsername(), LocalDateTime.now(),
                req.version() != null ? req.version() : existing.version());
        Organization saved = organizationRepository.update(updated);

        if ("PLANT".equals(existing.orgType())) {
            plantRepository.findByOrganizationId(id).ifPresent(plant -> {
                Plant updatedPlant = new Plant(plant.id(), plant.organizationId(),
                        plant.plantCode(),
                        req.plantName() != null ? req.plantName() : plant.plantName(),
                        req.timezone() != null ? req.timezone() : plant.timezone(),
                        req.country() != null ? req.country() : plant.country(),
                        req.address() != null ? req.address() : plant.address(),
                        plant.status(), plant.sourceType(), plant.sourceSystem(),
                        plant.externalId(), plant.externalVersion(), plant.lastSyncAt(),
                        plant.createdBy(), plant.createdAt(), currentUsername(), LocalDateTime.now(),
                        plant.version());
                plantRepository.save(updatedPlant);
            });
        } else if ("WAREHOUSE".equals(existing.orgType())) {
            warehouseRepository.findByOrganizationId(id).ifPresent(warehouse -> {
                Warehouse updatedWarehouse = new Warehouse(warehouse.id(), warehouse.organizationId(),
                        warehouse.warehouseCode(),
                        req.warehouseName() != null ? req.warehouseName() : warehouse.warehouseName(),
                        req.plantId() != null ? req.plantId() : warehouse.plantId(),
                        req.warehouseType() != null ? req.warehouseType() : warehouse.warehouseType(),
                        warehouse.status(), warehouse.sourceType(), warehouse.sourceSystem(),
                        warehouse.externalId(), warehouse.externalVersion(), warehouse.lastSyncAt(),
                        warehouse.createdBy(), warehouse.createdAt(), currentUsername(), LocalDateTime.now(),
                        warehouse.version());
                warehouseRepository.save(updatedWarehouse);
            });
        }

        audit.record("ORGANIZATION_UPDATED", "ORGANIZATION", String.valueOf(id),
                "SUCCESS", summary(existing), summary(saved), null);
        return toDetail(saved);
    }

    @Transactional
    public void enableOrganization(Long id) {
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Organization not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:enable", "masterdata", "ORGANIZATION"), id);
        if (!organizationRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("ORGANIZATION_ENABLED", "ORGANIZATION", String.valueOf(id),
                "SUCCESS", "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disableOrganization(Long id) {
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Organization not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:disable", "masterdata", "ORGANIZATION"), id);
        long activeChildren = organizationRepository.countByParentId(id);
        if (activeChildren > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable organization with active child organizations");
        }
        Map<String, Long> refs = organizationRepository.countActiveRefs(id);
        long totalRefs = refs.values().stream().mapToLong(Long::longValue).sum();
        if (totalRefs > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable organization with active departments, positions, users, plants, or warehouses");
        }
        if (!organizationRepository.updateStatus(id, "DISABLED", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("ORGANIZATION_DISABLED", "ORGANIZATION", String.valueOf(id),
                "SUCCESS", "status=" + existing.status(), "status=DISABLED", null);
    }

    @Transactional(readOnly = true)
    public List<OrganizationDetail> getValidParents(String orgType) {
        DataScopeResolution scope = dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION");
        String upper = orgType.toUpperCase();
        if ("GROUP".equals(upper)) {
            return Collections.emptyList();
        }
        String[] validTypes = switch (upper) {
            case "COMPANY" -> VALID_PARENT_FOR_COMPANY;
            case "BUSINESS_UNIT" -> VALID_PARENT_FOR_BUSINESS_UNIT;
            case "PLANT" -> VALID_PARENT_FOR_PLANT;
            case "WAREHOUSE" -> VALID_PARENT_FOR_WAREHOUSE;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid organization type: " + orgType);
        };
        return organizationRepository.findAll().stream()
                .filter(o -> covers(scope, o.id()))
                .filter(o -> java.util.Arrays.asList(validTypes).contains(o.orgType()))
                .filter(o -> "ACTIVE".equals(o.status()))
                .map(this::toDetail)
                .collect(Collectors.toList());
    }

    private void validateParentType(String orgType, Long parentId) {
        if ("GROUP".equals(orgType)) {
            if (parentId != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "GROUP organization cannot have a parent");
            }
            return;
        }
        if (parentId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, orgType + " organization must have a parent");
        }
        Organization parent = organizationRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent organization not found"));
        String[] valid = switch (orgType) {
            case "COMPANY" -> VALID_PARENT_FOR_COMPANY;
            case "BUSINESS_UNIT" -> VALID_PARENT_FOR_BUSINESS_UNIT;
            case "PLANT" -> VALID_PARENT_FOR_PLANT;
            case "WAREHOUSE" -> VALID_PARENT_FOR_WAREHOUSE;
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid organization type: " + orgType);
        };
        if (!java.util.Arrays.asList(valid).contains(parent.orgType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    orgType + " must have a parent of type " + String.join(" or ", valid));
        }
    }

    private OrganizationTreeNode toTreeNode(Organization org, Map<Long, List<Organization>> childrenMap) {
        List<OrganizationTreeNode> children = childrenMap.getOrDefault(org.id(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(Organization::sortOrder))
                .map(o -> toTreeNode(o, childrenMap))
                .collect(Collectors.toList());
        return new OrganizationTreeNode(org.id(), org.orgCode(), org.orgName(), org.orgType(),
                org.sortOrder(), org.status(), org.level(), children);
    }

    private OrganizationDetail toDetail(Organization org) {
        PlantInfo plantInfo = null;
        WarehouseInfo warehouseInfo = null;
        if ("PLANT".equals(org.orgType())) {
            Optional<Plant> plant = plantRepository.findByOrganizationId(org.id());
            plantInfo = plant.map(p -> new PlantInfo(p.id(), p.plantCode(), p.plantName(),
                    p.timezone(), p.country(), p.address())).orElse(null);
        } else if ("WAREHOUSE".equals(org.orgType())) {
            Optional<Warehouse> warehouse = warehouseRepository.findByOrganizationId(org.id());
            warehouseInfo = warehouse.map(w -> new WarehouseInfo(w.id(), w.warehouseCode(),
                    w.warehouseName(), w.plantId(), w.warehouseType())).orElse(null);
        }
        return new OrganizationDetail(org.id(), org.orgCode(), org.orgName(), org.orgType(),
                org.parentId(), org.sortOrder(), org.path(), org.level(), org.status(),
                org.description(), org.version(), plantInfo, warehouseInfo);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean covers(DataScopeResolution scope, Long organizationId) {
        return scope.isAllScope() || scope.coversOrganization(organizationId);
    }

    private void requireCovers(DataScopeResolution scope, Long organizationId) {
        if (!covers(scope, organizationId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private String summary(Organization organization) {
        return "code=" + organization.orgCode() + ",name=" + organization.orgName()
                + ",type=" + organization.orgType() + ",status=" + organization.status();
    }
}
