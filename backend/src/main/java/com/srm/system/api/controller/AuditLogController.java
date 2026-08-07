package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.system.application.service.SystemGovernanceFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/audit-logs")
public class AuditLogController {

    private final SystemGovernanceFacade governance;

    public AuditLogController(SystemGovernanceFacade governance) {
        this.governance = governance;
    }

    @GetMapping
    @Operation(summary = "审计日志分页查询")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:audit-log:view')")
    public ApiResponse<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String resultCode,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return ApiResponse.success(governance.auditLogs(page, pageSize, operatorName, actionCode,
                targetType, targetId, resultCode, traceId, from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审计日志详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:audit-log:view')")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.success(governance.auditLog(id));
    }
}
