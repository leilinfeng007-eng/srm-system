package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.application.service.ApprovalService;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.api.request.ApprovalDecisionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final SystemGovernanceFacade queryRepo;

    public ApprovalController(ApprovalService approvalService, SystemGovernanceFacade queryRepo) {
        this.approvalService = approvalService;
        this.queryRepo = queryRepo;
    }

    @GetMapping
    @Operation(summary = "我发起的审批分页")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:approval:view')")
    public ApiResponse<com.srm.common.api.PageResult<SystemGovernanceFacade.ApprovalView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.approvals(page, pageSize, status, p.userId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:approval:view')")
    public ApiResponse<SystemGovernanceFacade.ApprovalView> get(@PathVariable Long id) {
        return ApiResponse.success(queryRepo.approval(id, currentPrincipal().userId()));
    }

    @GetMapping("/{id}/nodes")
    @PreAuthorize("hasAuthority('system:approval:view')")
    public ApiResponse<java.util.List<SystemGovernanceFacade.ApprovalNodeView>> nodes(@PathVariable Long id) {
        return ApiResponse.success(queryRepo.approvalNodes(id, currentPrincipal().userId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('system:approval:approve')")
    public ApiResponse<Void> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalDecisionRequest body) {
        approvalService.approve(id, body != null && body.decision() != null ? body.decision() : "同意");
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('system:approval:reject')")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalDecisionRequest body) {
        approvalService.reject(id, body != null && body.reason() != null ? body.reason() : "驳回");
        return ApiResponse.success();
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAuthority('system:approval:withdraw')")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        approvalService.withdraw(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('system:approval:cancel')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        approvalService.cancel(id);
        return ApiResponse.success();
    }

    private SrmPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal p) return p;
        throw new IllegalStateException("Not authenticated");
    }
}
