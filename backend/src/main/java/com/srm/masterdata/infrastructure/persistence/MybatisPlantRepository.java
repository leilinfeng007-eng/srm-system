package com.srm.masterdata.infrastructure.persistence;

import com.srm.masterdata.domain.model.Plant;
import com.srm.masterdata.domain.repository.PlantRepository;
import com.srm.masterdata.infrastructure.persistence.entity.MdPlantEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdPlantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisPlantRepository implements PlantRepository {

    private final MdPlantMapper plantMapper;

    public MybatisPlantRepository(MdPlantMapper plantMapper) {
        this.plantMapper = plantMapper;
    }

    @Override
    public Optional<Plant> findByOrganizationId(Long orgId) {
        LambdaQueryWrapper<MdPlantEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdPlantEntity::getOrganizationId, orgId);
        MdPlantEntity entity = plantMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public Plant save(Plant plant) {
        MdPlantEntity entity = toEntity(plant);
        if (plant.id() == null) {
            plantMapper.insert(entity);
        } else if (plantMapper.updateById(entity) != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Plant was modified concurrently: " + plant.id());
        }
        return toDomain(entity);
    }

    private Plant toDomain(MdPlantEntity entity) {
        return new Plant(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getPlantCode(),
                entity.getPlantName(),
                entity.getTimezone(),
                entity.getCountry(),
                entity.getAddress(),
                entity.getStatus(),
                entity.getSourceType(),
                entity.getSourceSystem(),
                entity.getExternalId(),
                entity.getExternalVersion(),
                entity.getLastSyncAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    private MdPlantEntity toEntity(Plant plant) {
        MdPlantEntity entity = new MdPlantEntity();
        entity.setId(plant.id());
        entity.setOrganizationId(plant.organizationId());
        entity.setPlantCode(plant.plantCode());
        entity.setPlantName(plant.plantName());
        entity.setTimezone(plant.timezone());
        entity.setCountry(plant.country());
        entity.setAddress(plant.address());
        entity.setStatus(plant.status());
        entity.setSourceType(plant.sourceType());
        entity.setSourceSystem(plant.sourceSystem());
        entity.setExternalId(plant.externalId());
        entity.setExternalVersion(plant.externalVersion());
        entity.setLastSyncAt(plant.lastSyncAt());
        entity.setCreatedBy(plant.createdBy());
        entity.setCreatedAt(plant.createdAt());
        entity.setUpdatedBy(plant.updatedBy());
        entity.setUpdatedAt(plant.updatedAt());
        entity.setVersion(plant.version());
        return entity;
    }
}
