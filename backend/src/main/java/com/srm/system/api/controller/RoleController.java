package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.system.api.request.AssignUsersRequest;
import com.srm.system.api.request.CreateRoleRequest;
import com.srm.system.api.request.RoleDataPolicyRequest;
import com.srm.system.api.request.RoleMenuPermissionRequest;
import com.srm.system.api.request.UpdateRoleRequest;
import com.srm.system.api.response.RoleDetailResponse;
import com.srm.system.api.response.RoleHistoryResponse;
import com.srm.system.api.response.RoleResponse;
import com.srm.system.api.response.RoleUserResponse;
import com.srm.system.application.service.RoleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/roles")
public class RoleController {

    private final RoleApplicationService roleService;

    public RoleController(RoleApplicationService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "分页查询角色列表")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<PageResult<RoleResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(roleService.listRoles(page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<RoleDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @PostMapping
    @Operation(summary = "创建角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:create')")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:update')")
    public ApiResponse<RoleResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleService.updateRole(id, request));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:enable')")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        roleService.enableRole(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        roleService.disableRole(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/users")
    @Operation(summary = "批量分配用户到角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:assign-user')")
    public ApiResponse<Void> assignUsers(@PathVariable Long id,
                                          @Valid @RequestBody AssignUsersRequest request) {
        roleService.assignUsersToRole(id, request.userIds());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/users/{userId}")
    @Operation(summary = "从角色移除用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:assign-user')")
    public ApiResponse<Void> removeUser(@PathVariable Long id, @PathVariable Long userId) {
        roleService.removeUserFromRole(id, userId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/menus-permissions")
    @Operation(summary = "分配菜单和权限到角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:assign-permission')")
    public ApiResponse<Void> assignMenusPermissions(@PathVariable Long id,
                                                     @RequestBody RoleMenuPermissionRequest request) {
        roleService.assignMenusPermissions(id, request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/data-policies")
    @Operation(summary = "分配数据策略到角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:assign-data-scope')")
    public ApiResponse<Void> assignDataPolicies(@PathVariable Long id,
                                                 @Valid @RequestBody List<RoleDataPolicyRequest> policies) {
        roleService.assignDataPolicies(id, policies);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "获取角色变更历史")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<List<RoleHistoryResponse>> history(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRoleHistory(id));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "查询角色挂载用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<List<RoleUserResponse>> users(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRoleUsers(id));
    }
}
