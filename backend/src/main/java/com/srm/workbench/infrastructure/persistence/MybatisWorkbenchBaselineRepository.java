package com.srm.workbench.infrastructure.persistence;

import com.srm.workbench.domain.repository.WorkbenchBaselineRepository;
import com.srm.workbench.infrastructure.persistence.mapper.WorkbenchBaselineMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisWorkbenchBaselineRepository implements WorkbenchBaselineRepository {

    private final WorkbenchBaselineMapper mapper;

    public MybatisWorkbenchBaselineRepository(WorkbenchBaselineMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isDatabaseReachable() {
        return mapper.selectDatabaseProbe() == 1;
    }
}
