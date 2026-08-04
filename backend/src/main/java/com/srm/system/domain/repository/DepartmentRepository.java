package com.srm.system.domain.repository;

import com.srm.system.domain.model.Department;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DepartmentRepository {

    Optional<Department> findById(Long id);

    List<Department> findAll();

    List<Department> findByOrganizationId(Long organizationId);

    List<Department> findByParentId(Long parentId);

    Department save(Department department);

    Department update(Department department);

    boolean updateStatus(Long id, String status, Long version);

    boolean existsByDeptCode(String deptCode);

    long countByParentId(Long parentId);

    Map<String, Long> countActiveRefs(Long departmentId);
}
