package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.system.api.request.DocumentTemplateRequest;
import com.srm.system.api.request.TemplateAttachmentRequest;
import com.srm.system.application.service.AttachmentApplicationService;
import com.srm.system.application.service.SystemGovernanceFacade;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/document-templates")
public class DocumentTemplateController {
    private final SystemGovernanceFacade governance;
    private final AttachmentApplicationService attachments;

    public DocumentTemplateController(SystemGovernanceFacade governance,
                                      AttachmentApplicationService attachments) {
        this.governance = governance;
        this.attachments = attachments;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:template-attachment:view')")
    public ApiResponse<?> list(@RequestParam(defaultValue="1") int page,
                               @RequestParam(defaultValue="10") int pageSize,
                               @RequestParam(required=false) String templateCode) {
        return ApiResponse.success(governance.documentTemplates(page, pageSize, templateCode));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:template-attachment:view')")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.success(governance.documentTemplate(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:document-template:create')")
    public ApiResponse<?> create(@Valid @RequestBody DocumentTemplateRequest request) {
        return ApiResponse.success(governance.createDocumentTemplate(request.templateCode(),
                request.templateName(), request.purpose(), request.domainCode()));
    }

    @PutMapping("/{id}/attachment")
    @PreAuthorize("hasAuthority('system:document-template:update')")
    public ApiResponse<Void> bindAttachment(@PathVariable Long id,
                                            @Valid @RequestBody TemplateAttachmentRequest request) {
        governance.bindDocumentTemplateAttachment(id, request.attachmentId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:document-template:publish')")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        governance.publishDocumentTemplate(id); return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('system:document-template:disable')")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        governance.disableDocumentTemplate(id); return ApiResponse.success();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('system:document-template:download')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var file = attachments.download(governance.documentTemplateAttachmentId(id));
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, file.mimeType());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                + URLEncoder.encode(file.originalName(), StandardCharsets.UTF_8).replace("+", "%20"));
        return new ResponseEntity<>(file.content(), headers, HttpStatus.OK);
    }
}
