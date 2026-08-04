package com.srm.system.infrastructure.persistence;

import com.srm.masterdata.domain.model.Organization;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import com.srm.security.infrastructure.persistence.mapper.UserAccountMapper;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.repository.DataScopeRepository;
import com.srm.system.infrastructure.persistence.dto.DataScopeGrantRow;
import com.srm.system.infrastructure.persistence.mapper.DataScopeGrantMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisDataScopeRepository implements DataScopeRepository {

    private final DataScopeGrantMapper grantMapper;
    private final OrganizationRepository organizationRepository;
    private final UserAccountMapper userAccountMapper;

    public MybatisDataScopeRepository(DataScopeGrantMapper grantMapper,
                                       OrganizationRepository organizationRepository,
                                       UserAccountMapper userAccountMapper) {
        this.grantMapper = grantMapper;
        this.organizationRepository = organizationRepository;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public DataScopeResolution resolveDataScope(Long userId, String permissionCode,
                                                String domainCode, String dimensionCode,
                                                boolean writeOperation) {
        List<DataScopeGrantRow> grants = grantMapper.selectGrants(userId, permissionCode,
                domainCode, dimensionCode, writeOperation);
        if (grants.isEmpty()) {
            return DataScopeResolution.DENY;
        }
        Long userOrgId = resolveUserMainOrgId(userId);
        boolean all = grants.stream().anyMatch(g -> "ALL".equals(g.scopeType()));
        boolean org = grants.stream().anyMatch(g -> "ORG".equals(g.scopeType()));
        boolean self = grants.stream().anyMatch(g -> "SELF".equals(g.scopeType()));
        String mergedScope = all ? "ALL" : org ? "ORG" : self ? "SELF" : "DENY";
        if ("DENY".equals(mergedScope)) return DataScopeResolution.DENY;

        List<Long> orgIds = org && userOrgId != null
                ? resolveOrgWithChildren(userOrgId, grants.stream()
                        .filter(g -> "ORG".equals(g.scopeType()))
                        .anyMatch(g -> Boolean.TRUE.equals(g.includeChildren())))
                : List.of();
        return new DataScopeResolution(mergedScope, writeOperation, userOrgId,
                List.copyOf(orgIds), self ? userId : null);
    }

    @Override
    public Long resolveUserMainOrgId(Long userId) {
        SysUserEntity user = userAccountMapper.selectById(userId);
        return user != null ? user.getMainOrganizationId() : null;
    }

    private List<Long> resolveOrgWithChildren(Long orgId, boolean includeChildren) {
        if (orgId == null) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        result.add(orgId);
        if (includeChildren) collectChildren(orgId, result);
        return result;
    }

    private void collectChildren(Long parentId, List<Long> result) {
        List<Organization> children = organizationRepository.findByParentId(parentId).stream()
                .filter(o -> "ACTIVE".equals(o.status()))
                .toList();
        for (Organization child : children) {
            result.add(child.id());
            collectChildren(child.id(), result);
        }
    }
}
