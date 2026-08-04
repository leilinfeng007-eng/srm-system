package com.srm.system.domain.permission;

import java.util.List;

public record DataScopeResolution(
        String scopeType,
        boolean writable,
        Long userOrgId,
        List<Long> allowedOrgIds,
        Long ownerUserId) {

    public static final DataScopeResolution DENY = new DataScopeResolution(
            "DENY", false, null, List.of(), null);

    public boolean isAllScope() {
        return "ALL".equals(scopeType);
    }

    public boolean isOrgScope() {
        return "ORG".equals(scopeType);
    }

    public boolean isSelfScope() {
        return "SELF".equals(scopeType);
    }

    public boolean canView() {
        return !"DENY".equals(scopeType);
    }

    public boolean canWrite() {
        return canView() && writable;
    }

    public boolean coversOrganization(Long organizationId) {
        if (!canView() || organizationId == null) return false;
        return isAllScope() || (isOrgScope() && allowedOrgIds.contains(organizationId));
    }

    public boolean coversOwner(Long candidateOwnerUserId) {
        if (!canView() || candidateOwnerUserId == null) return false;
        return isAllScope() || (isSelfScope() && candidateOwnerUserId.equals(ownerUserId));
    }
}
