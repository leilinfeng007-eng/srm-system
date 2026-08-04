package com.srm.system.api.controller;
import com.srm.common.api.ApiResponse;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.api.request.ParameterRequest;
import com.srm.system.api.request.ParameterVersionRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/parameters")
public class ParameterController {
    private final SystemGovernanceFacade r;
    public ParameterController(SystemGovernanceFacade r) { this.r = r; }
    @GetMapping @PreAuthorize("hasAuthority('system:parameter:view')") public ApiResponse<?> list() { return ApiResponse.success(r.parameters()); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('system:parameter:view')") public ApiResponse<?> get(@PathVariable Long id) { return ApiResponse.success(r.parameter(id)); }
    @PostMapping @PreAuthorize("hasAuthority('system:parameter:create')") public ApiResponse<?> create(@RequestBody ParameterRequest b) { return ApiResponse.success(r.createParameter(b.paramCode(),b.paramName(),b.paramType(),b.defaultValue(),b.validationRule(),b.approvalRequired(),b.description())); }
    @GetMapping("/{pid}/versions") @PreAuthorize("hasAuthority('system:parameter:view')") public ApiResponse<?> versions(@PathVariable Long pid) { return ApiResponse.success(r.parameterVersions(pid)); }
    @PostMapping("/{pid}/versions") @PreAuthorize("hasAuthority('system:parameter:update')") public ApiResponse<?> createVersion(@PathVariable Long pid,@Valid @RequestBody ParameterVersionRequest b) { return ApiResponse.success(r.createParameterVersion(pid,b.paramValue())); }
    @PostMapping("/{pid}/versions/{vid}/submit") @PreAuthorize("hasAuthority('system:parameter:submit')") public ApiResponse<Void> submit(@PathVariable Long pid,@PathVariable Long vid) { r.submitParameterVersion(vid); return ApiResponse.success(); }
}
