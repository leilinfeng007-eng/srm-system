package com.srm.masterdata.application.service;

import com.srm.common.api.PageResult;
import com.srm.masterdata.application.command.MasterDataCommand;

public interface MasterDataAdministration {
    enum Resource { PURCHASING_ORGANIZATION, DELIVERY_LOCATION, CATEGORY, UNIT,
        CURRENCY, TAX_CODE, MATERIAL, EXTERNAL_MAPPING }

    PageResult<?> list(Resource resource, int page, int pageSize, String filter);
    PageResult<?> list(Resource resource, int page, int pageSize, String keyword,
                       String status, String sourceSystem);
    Object get(Resource resource, Long id);
    Object create(Resource resource, MasterDataCommand command);
    void update(Resource resource, Long id, MasterDataCommand command);
    void setStatus(Resource resource, Long id, String status);
    MasterDataBatchGateway.ExportPage exportPage(Resource resource, int page, int pageSize);
}
