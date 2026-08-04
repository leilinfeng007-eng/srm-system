package com.srm.masterdata.domain.repository;

import com.srm.platform.integration.InboxEventPayload;

public interface MasterDataSyncRepository {

    void apply(InboxEventPayload event);
}
