package com.srm.masterdata.infrastructure.persistence;

import com.srm.masterdata.domain.model.Organization;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.masterdata.infrastructure.persistence.entity.MdOrganizationEntity;
import com.srm.masterdata.infrastructure.persistence.mapper.MdOrganizationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisOrganizationRepository implements OrganizationRepository {

    private final MdOrganizationMapper organizationMapper;
    public MybatisOrganizationRepository(MdOrganizationMapper organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    @Override
    public Optional<Organization> findById(Long id) {
        MdOrganizationEntity entity = organizationMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Organization> findAll() {
        List<MdOrganizationEntity> entities = organizationMapper.selectList(null);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Organization> findByParentId(Long parentId) {
        LambdaQueryWrapper<MdOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdOrganizationEntity::getParentId, parentId);
        List<MdOrganizationEntity> entities = organizationMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Organization save(Organization org) {
        MdOrganizationEntity entity = toEntity(org);
        organizationMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    @Transactional
    public Organization update(Organization org) {
        MdOrganizationEntity entity = toEntity(org);
        if (organizationMapper.updateById(entity) != 1) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Organization was modified concurrently: " + org.id());
        }
        return toDomain(entity);
    }

    @Override
    public boolean updateStatus(Long id, String status, Long version) {
        LambdaUpdateWrapper<MdOrganizationEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MdOrganizationEntity::getId, id)
               .eq(MdOrganizationEntity::getVersion, version)
               .set(MdOrganizationEntity::getStatus, status)
               .setSql("version = version + 1");
        return organizationMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean existsByOrgCode(String orgCode) {
        LambdaQueryWrapper<MdOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdOrganizationEntity::getOrgCode, orgCode);
        return organizationMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countByParentId(Long parentId) {
        LambdaQueryWrapper<MdOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdOrganizationEntity::getParentId, parentId);
        return organizationMapper.selectCount(wrapper);
    }

    @Override
    public Map<String, Long> countActiveRefs(Long organizationId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("departments", nvl(organizationMapper.countActiveDepartments(organizationId)));
        counts.put("positions", nvl(organizationMapper.countActivePositions(organizationId)));
        counts.put("users", nvl(organizationMapper.countActiveUsers(organizationId)));
        counts.put("plants", nvl(organizationMapper.countActivePlants(organizationId)));
        counts.put("warehouses", nvl(organizationMapper.countActiveWarehouses(organizationId)));
        return counts;
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private Organization toDomain(MdOrganizationEntity entity) {
        return new Organization(
                entity.getId(),
                entity.getOrgCode(),
                entity.getOrgName(),
                entity.getOrgType(),
                entity.getParentId(),
                entity.getSortOrder(),
                entity.getPath(),
                entity.getLevel(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    private MdOrganizationEntity toEntity(Organization org) {
        MdOrganizationEntity entity = new MdOrganizationEntity();
        entity.setId(org.id());
        entity.setOrgCode(org.orgCode());
        entity.setOrgName(org.orgName());
        entity.setOrgType(org.orgType());
        entity.setParentId(org.parentId());
        entity.setSortOrder(org.sortOrder());
        entity.setPath(org.path());
        entity.setLevel(org.level());
        entity.setStatus(org.status());
        entity.setDescription(org.description());
        entity.setCreatedBy(org.createdBy());
        entity.setCreatedAt(org.createdAt());
        entity.setUpdatedBy(org.updatedBy());
        entity.setUpdatedAt(org.updatedAt());
        entity.setVersion(org.version());
        return entity;
    }
}
