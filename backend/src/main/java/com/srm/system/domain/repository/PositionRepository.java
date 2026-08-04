package com.srm.system.domain.repository;

import com.srm.system.domain.model.Position;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PositionRepository {

    Optional<Position> findById(Long id);

    List<Position> findAll();

    List<Position> findByDepartmentId(Long departmentId);

    Position save(Position position);

    Position update(Position position);

    boolean updateStatus(Long id, String status, Long version);

    boolean existsByPositionCode(String positionCode);

    long countByDepartmentId(Long departmentId);

    Map<String, Long> countActiveRefs(Long positionId);
}
