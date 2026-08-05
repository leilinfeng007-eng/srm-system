package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.system.api.request.CreateUserRequest;
import com.srm.system.api.request.UpdateUserRequest;
import com.srm.system.api.request.ChangePasswordRequest;
import com.srm.system.api.request.ReplaceUserRolesRequest;
import com.srm.system.api.response.AssignmentHistoryResponse;
import com.srm.system.api.response.AuthorizationRecordResponse;
import com.srm.system.api.response.ResetPasswordResponse;
import com.srm.system.api.response.UserDetailResponse;
import com.srm.system.api.response.UserEffectivePermissionsView;
import com.srm.system.api.response.UserResponse;
import com.srm.system.api.response.UserRoleSelectionResponse;
import com.srm.system.application.service.UserApplicationService;
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
@RequestMapping("/api/v1/system/users")
public class UserController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户列表")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<PageResult<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(userService.listUsers(page, pageSize, username, displayName, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<UserDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:create')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:update')")
    public ApiResponse<UserResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:enable')")
    public ApiResponse<Void> enable(@PathVariable Long id,
                                     @RequestParam(required = false) Long version) {
        userService.enableUser(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用用户")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id,
                                      @RequestParam(required = false) Long version) {
        userService.disableUser(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    public ApiResponse<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return ApiResponse.success(userService.resetPassword(id));
    }

    @GetMapping("/{id}/assignment-history")
    @Operation(summary = "获取用户组织/部门/岗位变更历史")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<List<AssignmentHistoryResponse>> assignmentHistory(@PathVariable Long id) {
        return ApiResponse.success(userService.getAssignmentHistory(id));
    }

    @GetMapping("/assignable-roles")
    @Operation(summary = "获取当前操作人可授予的角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:assign-role')")
    public ApiResponse<List<UserRoleSelectionResponse>> assignableRoles() {
        return ApiResponse.success(userService.getAssignableRoles());
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户角色分配与可分配角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<List<UserRoleSelectionResponse>> roles(@PathVariable Long id) {
        return ApiResponse.success(userService.getRoleSelection(id));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "批量分配用户角色")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:assign-role')")
    public ApiResponse<Void> replaceRoles(@PathVariable Long id,
                                           @Valid @RequestBody ReplaceUserRolesRequest request) {
        userService.replaceRoles(id, request.roleIds());
        return ApiResponse.success();
    }

    @GetMapping("/{id}/authorization-history")
    @Operation(summary = "获取用户授权变更记录")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view-authorization')")
    public ApiResponse<List<AuthorizationRecordResponse>> authorizationHistory(@PathVariable Long id) {
        return ApiResponse.success(userService.getAuthorizationHistory(id));
    }

    @GetMapping("/{id}/effective-permissions")
    @Operation(summary = "获取用户生效权限与数据范围")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:user:view-permissions')")
    public ApiResponse<UserEffectivePermissionsView> effectivePermissions(@PathVariable Long id) {
        return ApiResponse.success(userService.getEffectivePermissions(id));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改当前用户密码")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.oldPassword(), request.newPassword());
        return ApiResponse.success();
    }
}
