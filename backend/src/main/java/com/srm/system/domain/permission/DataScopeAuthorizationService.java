package com.srm.system.domain.permission;

import com.srm.security.auth.SrmPrincipal;
import com.srm.system.domain.repository.DataScopeRepository;
import java.util.List;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class DataScopeAuthorizationService {

    private final DataScopeRepository dataScopeRepository;

    public DataScopeAuthorizationService(DataScopeRepository dataScopeRepository) {
        this.dataScopeRepository = dataScopeRepository;
    }

    public DataScopeResolution resolveForRead(String permissionCode, String domainCode,
                                              String dimensionCode) {
        return resolve(permissionCode, domainCode, dimensionCode, false);
    }

    public DataScopeResolution resolveForWrite(String permissionCode, String domainCode,
                                               String dimensionCode) {
        return resolve(permissionCode, domainCode, dimensionCode, true);
    }

    public DataScopeResolution requireRead(String permissionCode, String domainCode,
                                           String dimensionCode) {
        DataScopeResolution resolution = resolveForRead(permissionCode, domainCode, dimensionCode);
        if (!resolution.canView()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return resolution;
    }

    public DataScopeResolution requireWrite(String permissionCode, String domainCode,
                                            String dimensionCode) {
        DataScopeResolution resolution = resolveForWrite(permissionCode, domainCode, dimensionCode);
        if (!resolution.canWrite()) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        return resolution;
    }

    private DataScopeResolution resolve(String permissionCode, String domainCode,
                                        String dimensionCode, boolean writeOperation) {
        SrmPrincipal principal = currentPrincipal();
        if (principal == null) {
            return DataScopeResolution.DENY;
        }
        return dataScopeRepository.resolveDataScope(principal.userId(), permissionCode,
                domainCode, dimensionCode, writeOperation);
    }

    private SrmPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal p) {
            return p;
        }
        return null;
    }
}
