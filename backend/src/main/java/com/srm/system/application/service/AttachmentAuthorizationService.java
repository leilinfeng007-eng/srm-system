package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.application.service.MasterDataOwnerAccessQueryService;
import com.srm.platform.attachment.AttachmentOwnerAccess;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.repository.SystemAttachmentOwnerRepository;
import com.srm.system.domain.repository.UserRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AttachmentAuthorizationService {

    private final MasterDataOwnerAccessQueryService masterDataOwners;
    private final SystemAttachmentOwnerRepository systemOwners;
    private final UserRepository userRepository;
    private final DataScopeAuthorizationService dataScope;

    public AttachmentAuthorizationService(MasterDataOwnerAccessQueryService masterDataOwners,
                                          SystemAttachmentOwnerRepository systemOwners,
                                          UserRepository userRepository,
                                          DataScopeAuthorizationService dataScope) {
        this.masterDataOwners = masterDataOwners;
        this.systemOwners = systemOwners;
        this.userRepository = userRepository;
        this.dataScope = dataScope;
    }

    public void require(String permissionCode, String ownerType, String ownerId, boolean write) {
        String normalizedType = ownerType == null ? "" : ownerType.trim().toUpperCase(Locale.ROOT);
        AttachmentOwnerAccess access = resolve(normalizedType, ownerId);
        DataScopeResolution scope = write
                ? dataScope.requireWrite(permissionCode, access.domainCode(), access.dimensionCode())
                : dataScope.requireRead(permissionCode, access.domainCode(), access.dimensionCode());
        boolean covered = scope.isAllScope()
                || (!access.globalOnly() && scope.isOrgScope()
                        && scope.coversOrganization(access.organizationId()))
                || (!access.globalOnly() && scope.isSelfScope()
                        && scope.coversOwner(access.ownerUserId()));
        if (!covered) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private AttachmentOwnerAccess resolve(String ownerType, String ownerId) {
        if ("USER".equals(ownerType)) {
            Long id = parseId(ownerId);
            return userRepository.findById(id)
                    .map(user -> new AttachmentOwnerAccess("system", "ORGANIZATION",
                            user.mainOrganizationId(), user.id(), false))
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        if ("BATCH_JOB".equals(ownerType) || "DOCUMENT_TEMPLATE".equals(ownerType)) {
            Long id = parseId(ownerId);
            var owner = systemOwners.findOwner(ownerType, id)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            if (owner.globalOnly()) {
                return new AttachmentOwnerAccess("system", "ORGANIZATION", null, null, true);
            }
            Long ownerUserId = userRepository.findByUsername(owner.username())
                    .map(user -> user.id()).orElse(null);
            return new AttachmentOwnerAccess("system", "OWNER", null, ownerUserId, false);
        }
        return masterDataOwners.resolve(ownerType, ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Unsupported or missing attachment owner"));
    }

    private Long parseId(String ownerId) {
        try {
            return Long.valueOf(ownerId);
        } catch (NumberFormatException invalid) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
