package com.srm.workbench.api.response;

import com.srm.workbench.domain.model.WorkbenchBaseline;

public record WorkbenchBaselineResponse(
        String moduleCode,
        String status,
        boolean databaseReachable) {

    public static WorkbenchBaselineResponse from(WorkbenchBaseline baseline) {
        return new WorkbenchBaselineResponse(
                baseline.moduleCode(),
                baseline.state().name(),
                baseline.databaseReachable());
    }
}
