package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.api.response.AttachmentResponse;
import com.srm.system.application.service.AttachmentApplicationService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/system/attachments")
public class AttachmentController {

    private final AttachmentApplicationService attachmentService;

    public AttachmentController(AttachmentApplicationService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:attachment:upload')")
    public ApiResponse<AttachmentResponse> upload(@RequestParam("file") MultipartFile file,
                                                  @RequestParam String ownerType,
                                                  @RequestParam String ownerId) {
        return ApiResponse.success(attachmentService.upload(file.getOriginalFilename(),
                file.getContentType(), bytes(file), ownerType, ownerId));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('system:attachment:download')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var download = attachmentService.download(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.mimeType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.originalName(), java.nio.charset.StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(download.content(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:attachment:view')")
    public ApiResponse<AttachmentResponse> get(@PathVariable Long id) {
        return ApiResponse.success(attachmentService.get(id));
    }

    @GetMapping("/by-owner")
    @PreAuthorize("hasAuthority('system:attachment:view')")
    public ApiResponse<List<AttachmentResponse>> listByOwner(@RequestParam String ownerType,
                                                             @RequestParam String ownerId) {
        return ApiResponse.success(attachmentService.listByOwner(ownerType, ownerId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:attachment:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:attachment:upload')")
    public ApiResponse<AttachmentResponse> replace(@PathVariable Long id,
                                                   @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(attachmentService.replace(id, file.getOriginalFilename(),
                file.getContentType(), bytes(file)));
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException readFailure) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unable to read uploaded file");
        }
    }
}
