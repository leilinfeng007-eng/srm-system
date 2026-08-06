package com.srm.system.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.masterdata.domain.model.Organization;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import com.srm.security.infrastructure.persistence.mapper.UserAccountMapper;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.repository.DataScopeRepository;
import com.srm.system.infrastructure.persistence.dto.DataScopeGrantRow;
import com.srm.system.infrastructure.persistence.mapper.DataScopeGrantMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisDataScopeRepository implements DataScopeRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        List<Long> orgIds = new ArrayList<>();
        if (org) {
            List<DataScopeGrantRow> orgGrants = grants.stream()
                    .filter(g -> "ORG".equals(g.scopeType()))
                    .toList();
            boolean hasScopeOrgIds = orgGrants.stream()
                    .anyMatch(g -> g.scopeOrgIds() != null && !g.scopeOrgIds().isBlank());
            if (hasScopeOrgIds) {
                Set<Long> allOrgIds = new HashSet<>();
                for (DataScopeGrantRow g : orgGrants) {
                    List<Long> ids = parseOrgIds(g.scopeOrgIds());
                    if (!ids.isEmpty()) {
                        if (Boolean.TRUE.equals(g.includeChildren())) {
                            for (Long oid : ids) {
                                allOrgIds.add(oid);
                                allOrgIds.addAll(resolveChildren(oid));
                            }
                        } else {
                            allOrgIds.addAll(ids);
                        }
                    }
                }
                if (userOrgId != null) {
                    boolean anyIncludeChildren = orgGrants.stream()
                            .anyMatch(g -> Boolean.TRUE.equals(g.includeChildren()));
                    allOrgIds.add(userOrgId);
                    if (anyIncludeChildren) {
                        allOrgIds.addAll(resolveChildren(userOrgId));
                    }
                }
                orgIds.addAll(allOrgIds);
            } else if (userOrgId != null) {
                boolean includeChildren = orgGrants.stream()
                        .anyMatch(g -> Boolean.TRUE.equals(g.includeChildren()));
                orgIds.add(userOrgId);
                if (includeChildren) {
                    orgIds.addAll(resolveChildren(userOrgId));
                }
            }
        }
        return new DataScopeResolution(mergedScope, writeOperation, userOrgId,
                List.copyOf(orgIds), self ? userId : null);
    }

    private List<Long> parseOrgIds(String scopeOrgIds) {
        if (scopeOrgIds == null || scopeOrgIds.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(scopeOrgIds, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> resolveChildren(Long orgId) {
        if (orgId == null) return new ArrayList<>();
        List<Long> result = new ArrayList<>();
        collectChildren(orgId, result);
        return result;
    }

    @Override
    public List<DataScopeResolution> resolvePerRoleDataScopes(Long userId, String permissionCode,
                                                               String domainCode, String dimensionCode,
                                                               boolean writeOperation) {
        List<DataScopeGrantRow> grants = grantMapper.selectGrants(userId, permissionCode,
                domainCode, dimensionCode, writeOperation);
        if (grants.isEmpty()) return List.of();

        Long userOrgId = resolveUserMainOrgId(userId);
        java.util.Map<Long, List<DataScopeGrantRow>> byRole = grants.stream()
                .collect(java.util.stream.Collectors.groupingBy(DataScopeGrantRow::roleId));

        List<DataScopeResolution> result = new ArrayList<>();
        for (var entry : byRole.entrySet()) {
            List<DataScopeGrantRow> roleGrants = entry.getValue();
            boolean all = roleGrants.stream().anyMatch(g -> "ALL".equals(g.scopeType()));
            boolean org = roleGrants.stream().anyMatch(g -> "ORG".equals(g.scopeType()));
            boolean self = roleGrants.stream().anyMatch(g -> "SELF".equals(g.scopeType()));
            String scope = all ? "ALL" : org ? "ORG" : self ? "SELF" : "DENY";
            if ("DENY".equals(scope)) continue;

            List<Long> orgIds = new ArrayList<>();
            if (org) {
                for (DataScopeGrantRow g : roleGrants) {
                    if (g.scopeOrgIds() != null && !g.scopeOrgIds().isBlank()) {
                        List<Long> ids = parseOrgIds(g.scopeOrgIds());
                        for (Long oid : ids) {
                            orgIds.add(oid);
                            if (Boolean.TRUE.equals(g.includeChildren())) {
                                orgIds.addAll(resolveChildren(oid));
                            }
                        }
                    }
                }
                if (orgIds.isEmpty() && userOrgId != null) {
                    orgIds.add(userOrgId);
                    boolean incChildren = roleGrants.stream().anyMatch(g -> Boolean.TRUE.equals(g.includeChildren()));
                    if (incChildren) orgIds.addAll(resolveChildren(userOrgId));
                }
            }
            result.add(new DataScopeResolution(scope, writeOperation, userOrgId,
                    List.copyOf(orgIds), self ? userId : null));
        }
        return result;
    }

    @Override
    public Long resolveUserMainOrgId(Long userId) {
        SysUserEntity user = userAccountMapper.selectById(userId);
        return user != null ? user.getMainOrganizationId() : null;
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
