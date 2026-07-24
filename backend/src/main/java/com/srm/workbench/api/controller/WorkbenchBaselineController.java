package com.srm.workbench.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.workbench.api.response.WorkbenchBaselineResponse;
import com.srm.workbench.application.query.WorkbenchBaselineQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbench")
public class WorkbenchBaselineController {

    private final WorkbenchBaselineQueryService queryService;

    public WorkbenchBaselineController(WorkbenchBaselineQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/baseline")
    @Operation(summary = "获取工作台架构基线")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('workbench:home:view')")
    public ApiResponse<WorkbenchBaselineResponse> baseline() {
        return ApiResponse.success(WorkbenchBaselineResponse.from(queryService.getBaseline()));
    }
}
