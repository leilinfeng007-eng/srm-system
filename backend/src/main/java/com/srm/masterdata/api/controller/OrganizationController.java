package com.srm.masterdata.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.masterdata.api.request.CreateOrganizationRequest;
import com.srm.masterdata.api.request.UpdateOrganizationRequest;
import com.srm.masterdata.api.response.OrganizationDetail;
import com.srm.masterdata.api.response.OrganizationTreeNode;
import com.srm.masterdata.application.service.OrganizationApplicationService;
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
@RequestMapping("/api/v1/master-data/organizations")
public class OrganizationController {

    private final OrganizationApplicationService organizationService;

    public OrganizationController(OrganizationApplicationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "获取组织架构树")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<List<OrganizationTreeNode>> listTree() {
        return ApiResponse.success(organizationService.listOrganizations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<OrganizationDetail> get(@PathVariable Long id) {
        return ApiResponse.success(organizationService.getOrganization(id));
    }

    @PostMapping
    @Operation(summary = "创建组织")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:create')")
    public ApiResponse<OrganizationDetail> create(@Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(organizationService.createOrganization(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:update')")
    public ApiResponse<OrganizationDetail> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateOrganizationRequest request) {
        return ApiResponse.success(organizationService.updateOrganization(id, request));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用组织")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:enable')")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        organizationService.enableOrganization(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用组织")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        organizationService.disableOrganization(id);
        return ApiResponse.success();
    }

    @GetMapping("/valid-parents")
    @Operation(summary = "获取合法的父组织候选")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('masterdata:organization:view')")
    public ApiResponse<List<OrganizationDetail>> validParents(@RequestParam String orgType) {
        return ApiResponse.success(organizationService.getValidParents(orgType));
    }
}
