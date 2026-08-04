package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.application.service.SystemGovernanceFacade.WorkflowNodeCommand;
import com.srm.system.api.request.WorkflowRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/workflows")
public class WorkflowController {

    private final SystemGovernanceFacade repo;
    public WorkflowController(SystemGovernanceFacade repo) { this.repo = repo; }

    @GetMapping
    @Operation(summary = "流程定义分页")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:workflow:view')")
    public ApiResponse<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(repo.workflows(page, pageSize, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:workflow:view')")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.success(repo.workflow(id));
    }

    @GetMapping("/{id}/nodes")
    @PreAuthorize("hasAuthority('system:workflow:view')")
    public ApiResponse<?> nodes(@PathVariable Long id) {
        return ApiResponse.success(repo.workflowNodes(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:workflow:create')")
    public ApiResponse<?> create(@RequestBody WorkflowRequest body) {
        return ApiResponse.success(repo.createWorkflow(
                body.processCode(), body.processName(), body.businessType(), body.description(), body.nodes()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:workflow:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody WorkflowRequest body) {
        repo.updateWorkflow(id, body.processName(), body.description(), body.nodes());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:workflow:publish')")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        repo.publishWorkflow(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/retire")
    @PreAuthorize("hasAuthority('system:workflow:retire')")
    public ApiResponse<Void> retire(@PathVariable Long id) {
        repo.retireWorkflow(id);
        return ApiResponse.success();
    }
}
