package com.srm.system.domain.repository;

import com.srm.system.domain.permission.DataScopeResolution;
import java.util.List;

public interface DataScopeRepository {

    DataScopeResolution resolveDataScope(Long userId, String permissionCode,
                                         String domainCode, String dimensionCode,
                                         boolean writeOperation);

    List<DataScopeResolution> resolvePerRoleDataScopes(Long userId, String permissionCode,
                                                        String domainCode, String dimensionCode,
                                                        boolean writeOperation);

    Long resolveUserMainOrgId(Long userId);
}
