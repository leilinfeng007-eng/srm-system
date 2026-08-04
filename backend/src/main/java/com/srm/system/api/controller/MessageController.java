package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.application.service.SystemGovernanceFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final SystemGovernanceFacade queryRepo;

    public MessageController(SystemGovernanceFacade queryRepo) { this.queryRepo = queryRepo; }

    @GetMapping("/my")
    @Operation(summary = "我的消息分页")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('system:message:view')")
    public ApiResponse<?> myMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.messages(page, pageSize, p.userId(), status));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('system:message:view')")
    public ApiResponse<Long> unreadCount() {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.unreadMessages(p.userId()));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('system:message:manage')")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        SrmPrincipal p = currentPrincipal();
        queryRepo.markMessageRead(id, p.userId());
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAuthority('system:message:manage')")
    public ApiResponse<Integer> markAllRead() {
        SrmPrincipal p = currentPrincipal();
        return ApiResponse.success(queryRepo.markAllMessagesRead(p.userId()));
    }

    private SrmPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal p) return p;
        throw new IllegalStateException("Not authenticated");
    }
}
