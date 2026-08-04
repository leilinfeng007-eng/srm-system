package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.system.api.request.CreatePositionRequest;
import com.srm.system.api.request.UpdatePositionRequest;
import com.srm.system.api.response.PositionResponse;
import com.srm.system.application.service.PositionApplicationService;
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
@RequestMapping("/api/v1/system/positions")
public class PositionController {

    private final PositionApplicationService positionService;

    public PositionController(PositionApplicationService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @Operation(summary = "按部门查询岗位列表")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<List<PositionResponse>> listByDepartment(@RequestParam Long departmentId) {
        return ApiResponse.success(positionService.listByDepartmentId(departmentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取岗位详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<PositionResponse> get(@PathVariable Long id) {
        return ApiResponse.success(positionService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建岗位")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:create')")
    public ApiResponse<PositionResponse> create(@Valid @RequestBody CreatePositionRequest request) {
        return ApiResponse.success(positionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新岗位")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:update')")
    public ApiResponse<PositionResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdatePositionRequest request) {
        return ApiResponse.success(positionService.update(id, request));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用岗位")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:enable')")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        positionService.enable(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用岗位")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        positionService.disable(id);
        return ApiResponse.success();
    }
}
