package com.srm.masterdata.infrastructure.persistence;

import com.srm.masterdata.domain.repository.MasterDataOwnerAccessRepository;
import com.srm.masterdata.infrastructure.persistence.entity.MdCategoryEntity;
import com.srm.masterdata.infrastructure.persistence.entity.MdPlantEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdCategoryMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdDeliveryLocationMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdMaterialMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdOrganizationMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdPlantMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdPurchasingOrganizationMapper;
import com.srm.masterdata.infrastructure.persistence.mapper.MdWarehouseMapper;
import com.srm.platform.attachment.AttachmentOwnerAccess;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisMasterDataOwnerAccessRepository implements MasterDataOwnerAccessRepository {

    private final MdOrganizationMapper organizationMapper;
    private final MdPlantMapper plantMapper;
    private final MdWarehouseMapper warehouseMapper;
    private final MdPurchasingOrganizationMapper purchasingOrganizationMapper;
    private final MdDeliveryLocationMapper deliveryLocationMapper;
    private final MdCategoryMapper categoryMapper;
    private final MdMaterialMapper materialMapper;

    public MybatisMasterDataOwnerAccessRepository(MdOrganizationMapper organizationMapper,
                                                  MdPlantMapper plantMapper,
                                                  MdWarehouseMapper warehouseMapper,
                                                  MdPurchasingOrganizationMapper purchasingOrganizationMapper,
                                                  MdDeliveryLocationMapper deliveryLocationMapper,
                                                  MdCategoryMapper categoryMapper,
                                                  MdMaterialMapper materialMapper) {
        this.organizationMapper = organizationMapper;
        this.plantMapper = plantMapper;
        this.warehouseMapper = warehouseMapper;
        this.purchasingOrganizationMapper = purchasingOrganizationMapper;
        this.deliveryLocationMapper = deliveryLocationMapper;
        this.categoryMapper = categoryMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    public Optional<AttachmentOwnerAccess> resolve(String ownerType, String ownerId) {
        Long id;
        try {
            id = Long.valueOf(ownerId);
        } catch (NumberFormatException invalidId) {
            return Optional.empty();
        }
        return switch (ownerType) {
            case "ORGANIZATION" -> Optional.ofNullable(organizationMapper.selectById(id))
                    .map(entity -> access("ORGANIZATION", entity.getId(), null));
            case "PLANT" -> Optional.ofNullable(plantMapper.selectById(id))
                    .map(entity -> access("PLANT", entity.getOrganizationId(), null));
            case "WAREHOUSE" -> Optional.ofNullable(warehouseMapper.selectById(id))
                    .flatMap(entity -> Optional.ofNullable(plantMapper.selectById(entity.getPlantId())))
                    .map(entity -> access("PLANT", entity.getOrganizationId(), null));
            case "PURCHASING_ORGANIZATION" -> Optional.ofNullable(
                            purchasingOrganizationMapper.selectById(id))
                    .map(entity -> access("PURCHASING_ORGANIZATION", entity.getCompanyOrgId(),
                            entity.getResponsibleUserId()));
            case "DELIVERY_LOCATION" -> Optional.ofNullable(deliveryLocationMapper.selectById(id))
                    .flatMap(entity -> Optional.ofNullable(plantMapper.selectById(entity.getPlantId())))
                    .map(entity -> access("PLANT", entity.getOrganizationId(), null));
            case "CATEGORY" -> Optional.ofNullable(categoryMapper.selectById(id))
                    .map(this::categoryAccess);
            case "MATERIAL" -> Optional.ofNullable(materialMapper.selectById(id))
                    .flatMap(entity -> Optional.ofNullable(categoryMapper.selectById(entity.getCategoryId())))
                    .map(this::categoryAccess);
            default -> Optional.empty();
        };
    }

    private AttachmentOwnerAccess categoryAccess(MdCategoryEntity category) {
        return access("CATEGORY", category.getResponsibleOrgId(), category.getCategoryManagerId());
    }

    private AttachmentOwnerAccess access(String dimension, Long organizationId, Long ownerUserId) {
        return new AttachmentOwnerAccess("masterdata", dimension, organizationId, ownerUserId, false);
    }
}
