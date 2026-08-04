package com.srm.masterdata.application.service;

import com.srm.masterdata.domain.repository.MasterDataSyncRepository;
import com.srm.platform.integration.InboxEventHandler;
import com.srm.platform.integration.InboxEventPayload;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MasterDataInboxEventHandler implements InboxEventHandler {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "PURCHASING_ORGANIZATION", "DELIVERY_LOCATION", "CATEGORY",
            "UNIT", "CURRENCY", "TAX_CODE", "MATERIAL");

    private final MasterDataSyncRepository syncRepository;

    public MasterDataInboxEventHandler(MasterDataSyncRepository syncRepository) {
        this.syncRepository = syncRepository;
    }

    @Override
    public boolean supports(String objectType) {
        return SUPPORTED_TYPES.contains(objectType);
    }

    @Override
    public void handle(InboxEventPayload event) {
        syncRepository.apply(event);
    }
}
