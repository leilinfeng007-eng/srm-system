package com.srm.platform.meta;

import com.srm.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final ModuleCatalog moduleCatalog;

    public MetaController(ModuleCatalog moduleCatalog) {
        this.moduleCatalog = moduleCatalog;
    }

    @GetMapping("/modules")
    @Operation(summary = "获取后端模块元数据")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:permission:view')")
    public ApiResponse<List<ModuleMeta>> modules() {
        return ApiResponse.success(moduleCatalog.all());
    }
}
