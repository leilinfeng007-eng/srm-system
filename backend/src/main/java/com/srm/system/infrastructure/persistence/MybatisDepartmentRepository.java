package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.model.Department;
import com.srm.system.domain.repository.DepartmentRepository;
import com.srm.system.infrastructure.persistence.entity.SysDepartmentEntity;
import com.srm.system.infrastructure.persistence.mapper.DepartmentMapper;
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
public class MybatisDepartmentRepository implements DepartmentRepository {

    private final DepartmentMapper departmentMapper;

    public MybatisDepartmentRepository(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Optional<Department> findById(Long id) {
        SysDepartmentEntity entity = departmentMapper.selectById(id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Department> findAll() {
        List<SysDepartmentEntity> entities = departmentMapper.selectList(null);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Department> findByOrganizationId(Long organizationId) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getOrganizationId, organizationId);
        List<SysDepartmentEntity> entities = departmentMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Department> findByParentId(Long parentId) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getParentId, parentId);
        List<SysDepartmentEntity> entities = departmentMapper.selectList(wrapper);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Department save(Department department) {
        SysDepartmentEntity entity = toEntity(department);
        departmentMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public Department update(Department department) {
        SysDepartmentEntity entity = toEntity(department);
        departmentMapper.updateById(entity);
        return toDomain(entity);
    }

    @Override
    public boolean updateStatus(Long id, String status, Long version) {
        LambdaUpdateWrapper<SysDepartmentEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysDepartmentEntity::getId, id)
               .eq(SysDepartmentEntity::getVersion, version)
               .set(SysDepartmentEntity::getStatus, status)
               .setSql("version = version + 1");
        return departmentMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean existsByDeptCode(String deptCode) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getDeptCode, deptCode);
        return departmentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countByParentId(Long parentId) {
        LambdaQueryWrapper<SysDepartmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDepartmentEntity::getParentId, parentId);
        return departmentMapper.selectCount(wrapper);
    }

    @Override
    public Map<String, Long> countActiveRefs(Long departmentId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("positions", nvl(departmentMapper.countActivePositions(departmentId)));
        counts.put("users", nvl(departmentMapper.countActiveUsers(departmentId)));
        return counts;
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private Department toDomain(SysDepartmentEntity entity) {
        return new Department(
                entity.getId(),
                entity.getDeptCode(),
                entity.getDeptName(),
                entity.getOrganizationId(),
                entity.getParentId(),
                entity.getManagerName(),
                entity.getManagerId(),
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

    private SysDepartmentEntity toEntity(Department department) {
        SysDepartmentEntity entity = new SysDepartmentEntity();
        entity.setId(department.id());
        entity.setDeptCode(department.deptCode());
        entity.setDeptName(department.deptName());
        entity.setOrganizationId(department.organizationId());
        entity.setParentId(department.parentId());
        entity.setManagerName(department.managerName());
        entity.setManagerId(department.managerId());
        entity.setSortOrder(department.sortOrder());
        entity.setPath(department.path());
        entity.setLevel(department.level());
        entity.setStatus(department.status());
        entity.setDescription(department.description());
        entity.setCreatedBy(department.createdBy());
        entity.setCreatedAt(department.createdAt());
        entity.setUpdatedBy(department.updatedBy());
        entity.setUpdatedAt(department.updatedAt());
        entity.setVersion(department.version());
        return entity;
    }
}
