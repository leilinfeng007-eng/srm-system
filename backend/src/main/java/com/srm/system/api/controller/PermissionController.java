package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.system.api.response.MenuCatalogNode;
import com.srm.system.api.response.UserEffectivePermissionsResponse;
import com.srm.system.application.service.AuthorizationCatalogApplicationService;
import com.srm.system.domain.model.PermissionCatalogItem;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class PermissionController {
    private final AuthorizationCatalogApplicationService service;

    public PermissionController(AuthorizationCatalogApplicationService service) {
        this.service = service;
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public ApiResponse<PageResult<PermissionCatalogItem>> permissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.permissions(page, pageSize));
    }

    @GetMapping("/permissions/effective-users/{userId}")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public ApiResponse<UserEffectivePermissionsResponse> effective(@PathVariable Long userId) {
        return ApiResponse.success(service.effectivePermissions(userId));
    }

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('system:role:view')")
    public ApiResponse<List<MenuCatalogNode>> menus() {
        return ApiResponse.success(service.menus());
    }
}
