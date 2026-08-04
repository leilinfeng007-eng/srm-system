package com.srm.masterdata.domain.repository;

import com.srm.platform.attachment.AttachmentOwnerAccess;
import java.util.Optional;

public interface MasterDataOwnerAccessRepository {

    Optional<AttachmentOwnerAccess> resolve(String ownerType, String ownerId);
}
