package com.srm.workbench.application.service;

import com.srm.workbench.application.query.WorkbenchBaselineQueryService;
import com.srm.workbench.domain.model.BaselineState;
import com.srm.workbench.domain.model.WorkbenchBaseline;
import com.srm.workbench.domain.repository.WorkbenchBaselineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkbenchBaselineService implements WorkbenchBaselineQueryService {

    private final WorkbenchBaselineRepository repository;

    public WorkbenchBaselineService(WorkbenchBaselineRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkbenchBaseline getBaseline() {
        boolean databaseReachable = repository.isDatabaseReachable();
        return new WorkbenchBaseline("workbench", BaselineState.READY, databaseReachable);
    }
}
