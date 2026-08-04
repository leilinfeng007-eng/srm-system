package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.system.api.request.CreateDepartmentRequest;
import com.srm.system.api.request.UpdateDepartmentRequest;
import com.srm.system.api.response.DepartmentResponse;
import com.srm.system.application.service.DepartmentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/departments")
public class DepartmentController {

    private final DepartmentApplicationService departmentService;

    public DepartmentController(DepartmentApplicationService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "按组织查询部门列表")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<List<DepartmentResponse>> listByOrganization(@RequestParam Long organizationId) {
        return ApiResponse.success(departmentService.listByOrganizationId(organizationId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取部门详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建部门")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:create')")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新部门")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:update')")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用部门")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:enable')")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        departmentService.enable(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用部门")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        departmentService.disable(id);
        return ApiResponse.success();
    }
}
