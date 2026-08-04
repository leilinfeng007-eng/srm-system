package com.srm.masterdata.domain.repository;

import com.srm.masterdata.domain.model.Organization;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrganizationRepository {

    Optional<Organization> findById(Long id);

    List<Organization> findAll();

    List<Organization> findByParentId(Long parentId);

    Organization save(Organization org);

    Organization update(Organization org);

    boolean updateStatus(Long id, String status, Long version);

    boolean existsByOrgCode(String orgCode);

    long countByParentId(Long parentId);

    Map<String, Long> countActiveRefs(Long organizationId);
}
