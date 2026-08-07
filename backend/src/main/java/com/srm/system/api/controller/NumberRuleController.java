package com.srm.system.api.controller;
import com.srm.common.api.ApiResponse;
import com.srm.system.application.service.SystemGovernanceFacade;
import com.srm.system.api.request.NumberRuleRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/number-rules")
public class NumberRuleController {
    private final SystemGovernanceFacade r;
    public NumberRuleController(SystemGovernanceFacade r) { this.r = r; }
    @GetMapping @PreAuthorize("hasAuthority('system:number-rule:view')") public ApiResponse<?> list() { return ApiResponse.success(r.numberRules()); }
    @PostMapping @PreAuthorize("hasAuthority('system:number-rule:create')") public ApiResponse<?> create(@RequestBody NumberRuleRequest b) { return ApiResponse.success(r.createNumberRule(b.ruleCode(),b.ruleName(),b.objectType(),b.prefix(),b.dateFormat(),b.serialLength(),b.resetCycle(),b.organizationDimension())); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('system:number-rule:update')") public ApiResponse<Void> update(@PathVariable Long id,@RequestBody NumberRuleRequest b) { r.updateNumberRule(id,b.ruleName(),b.prefix(),b.dateFormat(),b.serialLength(),b.resetCycle()); return ApiResponse.success(); }
    @PostMapping("/{id}/enable") @PreAuthorize("hasAuthority('system:number-rule:enable')") public ApiResponse<Void> enable(@PathVariable Long id) { r.setNumberRuleStatus(id,"ACTIVE"); return ApiResponse.success(); }
    @PostMapping("/{id}/disable") @PreAuthorize("hasAuthority('system:number-rule:disable')") public ApiResponse<Void> disable(@PathVariable Long id) { r.setNumberRuleStatus(id,"INACTIVE"); return ApiResponse.success(); }
    @PostMapping("/generate") @PreAuthorize("hasAuthority('system:number-rule:view')") public ApiResponse<String> generate(@RequestParam String ruleCode,@RequestParam(required=false) Long orgId) { return ApiResponse.success(r.generateNumber(ruleCode,orgId)); }
    @PostMapping("/preview") @PreAuthorize("hasAuthority('system:number-rule:view')") public ApiResponse<String> preview(@RequestBody com.srm.system.api.request.NumberRulePreviewRequest b) { return ApiResponse.success(r.previewNumber(b.ruleCode(),b.orgId())); }
}
