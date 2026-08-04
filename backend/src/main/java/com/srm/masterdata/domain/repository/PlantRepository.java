package com.srm.masterdata.domain.repository;

import com.srm.masterdata.domain.model.Plant;
import java.util.Optional;

public interface PlantRepository {

    Optional<Plant> findByOrganizationId(Long orgId);

    Plant save(Plant plant);
}
