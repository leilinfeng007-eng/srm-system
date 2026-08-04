package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.application.service.SystemGovernanceFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final SystemGovernanceFacade queryRepo;

    public TaskController(SystemGovernanceFacade queryRepo) { this.queryRepo = queryRepo; }

    @GetMapping("/my")
    @Operation(summary = "我的任务分页")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:task:view')")
    public ApiResponse<com.srm.common.api.PageResult<SystemGovernanceFacade.TaskView>> myTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.tasks(page, pageSize, p.userId(), status));
    }

    @GetMapping("/overdue-count")
    @PreAuthorize("hasAuthority('system:task:view')")
    public ApiResponse<Long> overdueCount() {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.overdueTasks(p.userId()));
    }

    private SrmPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal p) return p;
        throw new IllegalStateException("Not authenticated");
    }
}
