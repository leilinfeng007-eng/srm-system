package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.system.api.request.InboxEventRequest;
import com.srm.system.api.request.OutboxEventRequest;
import com.srm.system.application.service.IntegrationApplicationService;
import com.srm.system.domain.model.InboxEvent;
import com.srm.system.domain.model.OutboxEvent;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class IntegrationController {

    private final IntegrationApplicationService integrationService;
    private final com.srm.system.application.service.SystemGovernanceFacade queryRepo;

    public IntegrationController(IntegrationApplicationService integrationService,
                                 com.srm.system.application.service.SystemGovernanceFacade queryRepo) {
        this.integrationService = integrationService;
        this.queryRepo = queryRepo;
    }

    @GetMapping("/inbox-events")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<PageResult<InboxEvent>> listInbox(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return ApiResponse.success(integrationService.listInbox(page, size, status, sourceSystem, objectType, from, to));
    }

    @GetMapping("/outbox-events")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<PageResult<OutboxEvent>> listOutbox(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to) {
        return ApiResponse.success(integrationService.listOutbox(page, size, status, eventType, objectType, from, to));
    }

    @GetMapping("/inbox-events/{id}")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<InboxEvent> getInbox(@PathVariable Long id) {
        return ApiResponse.success(integrationService.getInbox(id));
    }

    @GetMapping("/outbox-events/{id}")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<OutboxEvent> getOutbox(@PathVariable Long id) {
        return ApiResponse.success(integrationService.getOutbox(id));
    }

    @GetMapping("/inbox-events/{id}/attempts")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<java.util.List<?>> inboxAttempts(@PathVariable Long id) {
        integrationService.getInbox(id);
        return ApiResponse.success(queryRepo.eventAttempts("INBOX_EVENT", String.valueOf(id)));
    }

    @GetMapping("/outbox-events/{id}/attempts")
    @PreAuthorize("hasAuthority('system:integration-job:view')")
    public ApiResponse<java.util.List<?>> outboxAttempts(@PathVariable Long id) {
        integrationService.getOutbox(id);
        return ApiResponse.success(queryRepo.eventAttempts("OUTBOX_EVENT", String.valueOf(id)));
    }

    @PostMapping("/inbox-events/{id}/retry")
    @PreAuthorize("hasAuthority('system:integration-job:retry')")
    public ApiResponse<InboxEvent> retryInbox(@PathVariable Long id) {
        return ApiResponse.success(integrationService.retryInbox(id));
    }

    @PostMapping("/outbox-events/{id}/retry")
    @PreAuthorize("hasAuthority('system:integration-job:retry')")
    public ApiResponse<OutboxEvent> retryOutbox(@PathVariable Long id) {
        return ApiResponse.success(integrationService.retryOutbox(id));
    }

    @PostMapping("/outbox-events")
    @PreAuthorize("hasAuthority('system:integration-job:retry')")
    public ApiResponse<OutboxEvent> createOutbox(@Valid @RequestBody OutboxEventRequest request) {
        return ApiResponse.success(integrationService.createOutbox(request));
    }

    @PostMapping("/inbox-events")
    @PreAuthorize("hasAuthority('system:integration-job:retry')")
    public ApiResponse<InboxEvent> receiveInbox(@Valid @RequestBody InboxEventRequest request) {
        return ApiResponse.success(integrationService.receiveInbox(request));
    }
}
