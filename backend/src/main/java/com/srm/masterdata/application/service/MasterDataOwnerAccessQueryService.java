package com.srm.masterdata.application.service;

import com.srm.masterdata.domain.repository.MasterDataOwnerAccessRepository;
import com.srm.platform.attachment.AttachmentOwnerAccess;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataOwnerAccessQueryService {

    private final MasterDataOwnerAccessRepository repository;

    public MasterDataOwnerAccessQueryService(MasterDataOwnerAccessRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<AttachmentOwnerAccess> resolve(String ownerType, String ownerId) {
        return repository.resolve(ownerType, ownerId);
    }
}
