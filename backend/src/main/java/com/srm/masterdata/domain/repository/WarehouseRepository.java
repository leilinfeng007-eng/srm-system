package com.srm.masterdata.domain.repository;

import com.srm.masterdata.domain.model.Warehouse;
import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {

    Optional<Warehouse> findByOrganizationId(Long orgId);

    List<Warehouse> findByPlantId(Long plantId);

    Warehouse save(Warehouse warehouse);
}
