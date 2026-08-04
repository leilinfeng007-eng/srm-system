package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.model.Position;
import com.srm.system.domain.repository.PositionRepository;
import com.srm.system.infrastructure.persistence.entity.SysPositionEntity;
import com.srm.system.infrastructure.persistence.mapper.PositionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisPositionRepository implements PositionRepository {

    private final PositionMapper positionMapper;

    public MybatisPositionRepository(PositionMapper positionMapper) {
        this.positionMapper = positionMapper;
    }

    @Override
    public Optional<Position> findById(Long id) {
        SysPositionEntity entity = positionMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Position> findAll() {
        List<SysPositionEntity> entities = positionMapper.selectList(null);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Position> findByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<SysPositionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPositionEntity::getDepartmentId, departmentId);
        List<SysPositionEntity> entities = positionMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Position save(Position position) {
        SysPositionEntity entity = toEntity(position);
        positionMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Position update(Position position) {
        SysPositionEntity entity = toEntity(position);
        positionMapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public boolean updateStatus(Long id, String status, Long version) {
        LambdaUpdateWrapper<SysPositionEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysPositionEntity::getId, id)
               .eq(SysPositionEntity::getVersion, version)
               .set(SysPositionEntity::getStatus, status)
               .setSql("version = version + 1");
        return positionMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean existsByPositionCode(String positionCode) {
        LambdaQueryWrapper<SysPositionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPositionEntity::getPositionCode, positionCode);
        return positionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<SysPositionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPositionEntity::getDepartmentId, departmentId);
        return positionMapper.selectCount(wrapper);
    }

    @Override
    public Map<String, Long> countActiveRefs(Long positionId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", nvl(positionMapper.countActiveUsers(positionId)));
        return counts;
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private Position toDomain(SysPositionEntity entity) {
        return new Position(
                entity.getId(),
                entity.getPositionCode(),
                entity.getPositionName(),
                entity.getDepartmentId(),
                entity.getResponsibility(),
                entity.getSortOrder(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    private SysPositionEntity toEntity(Position position) {
        SysPositionEntity entity = new SysPositionEntity();
        entity.setId(position.id());
        entity.setPositionCode(position.positionCode());
        entity.setPositionName(position.positionName());
        entity.setDepartmentId(position.departmentId());
        entity.setResponsibility(position.responsibility());
        entity.setSortOrder(position.sortOrder());
        entity.setStatus(position.status());
        entity.setCreatedBy(position.createdBy());
        entity.setCreatedAt(position.createdAt());
        entity.setUpdatedBy(position.updatedBy());
        entity.setUpdatedAt(position.updatedAt());
        entity.setVersion(position.version());
        return entity;
    }
}
