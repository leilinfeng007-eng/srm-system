package com.srm.masterdata.infrastructure.persistence;

import com.srm.masterdata.domain.model.Warehouse;
import com.srm.masterdata.domain.repository.WarehouseRepository;
import com.srm.masterdata.infrastructure.persistence.entity.MdWarehouseEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdWarehouseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisWarehouseRepository implements WarehouseRepository {

    private final MdWarehouseMapper warehouseMapper;

    public MybatisWarehouseRepository(MdWarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public Optional<Warehouse> findByOrganizationId(Long orgId) {
        LambdaQueryWrapper<MdWarehouseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdWarehouseEntity::getOrganizationId, orgId);
        MdWarehouseEntity entity = warehouseMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Warehouse> findByPlantId(Long plantId) {
        LambdaQueryWrapper<MdWarehouseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdWarehouseEntity::getPlantId, plantId);
        List<MdWarehouseEntity> entities = warehouseMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        MdWarehouseEntity entity = toEntity(warehouse);
        if (warehouse.id() == null) {
            warehouseMapper.insert(entity);
        } else if (warehouseMapper.updateById(entity) != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Warehouse was modified concurrently: " + warehouse.id());
        }
        return toDomain(entity);
    }

    private Warehouse toDomain(MdWarehouseEntity entity) {
        return new Warehouse(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getWarehouseCode(),
                entity.getWarehouseName(),
                entity.getPlantId(),
                entity.getWarehouseType(),
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

    private MdWarehouseEntity toEntity(Warehouse warehouse) {
        MdWarehouseEntity entity = new MdWarehouseEntity();
        entity.setId(warehouse.id());
        entity.setOrganizationId(warehouse.organizationId());
        entity.setWarehouseCode(warehouse.warehouseCode());
        entity.setWarehouseName(warehouse.warehouseName());
        entity.setPlantId(warehouse.plantId());
        entity.setWarehouseType(warehouse.warehouseType());
        entity.setStatus(warehouse.status());
        entity.setSourceType(warehouse.sourceType());
        entity.setSourceSystem(warehouse.sourceSystem());
        entity.setExternalId(warehouse.externalId());
        entity.setExternalVersion(warehouse.externalVersion());
        entity.setLastSyncAt(warehouse.lastSyncAt());
        entity.setCreatedBy(warehouse.createdBy());
        entity.setCreatedAt(warehouse.createdAt());
        entity.setUpdatedBy(warehouse.updatedBy());
        entity.setUpdatedAt(warehouse.updatedAt());
        entity.setVersion(warehouse.version());
        return entity;
    }
}
