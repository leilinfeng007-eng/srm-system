package com.srm.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.workbench.application.service.WorkbenchBaselineService;
import com.srm.workbench.domain.model.BaselineState;
import org.junit.jupiter.api.Test;

class WorkbenchBaselineServiceTest {

    @Test
    void mapsTheRepositoryProbeToTheReferenceDomainValue() {
        WorkbenchBaselineService service = new WorkbenchBaselineService(() -> true);

        var baseline = service.getBaseline();

        assertThat(baseline.moduleCode()).isEqualTo("workbench");
        assertThat(baseline.state()).isEqualTo(BaselineState.READY);
        assertThat(baseline.databaseReachable()).isTrue();
    }
}
