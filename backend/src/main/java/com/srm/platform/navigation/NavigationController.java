package com.srm.platform.navigation;

import com.srm.common.api.ApiResponse;
import com.srm.security.auth.SrmPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/navigation")
public class NavigationController {

    private final NavigationQueryService navigation;

    public NavigationController(NavigationQueryService navigation) {
        this.navigation = navigation;
    }

    @GetMapping("/menus")
    @Operation(summary = "获取当前用户动态菜单")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<MenuNode>> menus(@AuthenticationPrincipal SrmPrincipal principal) {
        return ApiResponse.success(navigation.findGrantedTree(principal.userId()));
    }
}
