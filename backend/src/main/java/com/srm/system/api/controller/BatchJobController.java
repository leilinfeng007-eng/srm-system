package com.srm.system.api.controller;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.system.api.request.BatchExportRequest;
import com.srm.system.application.service.BatchJobApplicationService;
import com.srm.system.domain.model.BatchJob;
import com.srm.system.domain.model.BatchJobError;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/system/batch-jobs")
public class BatchJobController {
    private final BatchJobApplicationService service;
    public BatchJobController(BatchJobApplicationService service){this.service=service;}

    @GetMapping @PreAuthorize("hasAuthority('system:batch-job:view')")
    public ApiResponse<PageResult<BatchJob>> list(@RequestParam(defaultValue="1")int page,
            @RequestParam(defaultValue="10")int pageSize,@RequestParam(required=false)String objectType){return ApiResponse.success(service.list(page,pageSize,objectType));}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('system:batch-job:view')")
    public ApiResponse<BatchJob> get(@PathVariable Long id){return ApiResponse.success(service.get(id));}
    @GetMapping("/{id}/errors") @PreAuthorize("hasAuthority('system:batch-job:view')")
    public ApiResponse<List<BatchJobError>> errors(@PathVariable Long id){return ApiResponse.success(service.errors(id));}

    @GetMapping("/import-template") @PreAuthorize("hasAuthority('system:batch-job:view')")
    public ResponseEntity<byte[]> template(@RequestParam String objectType){byte[] bytes=service.template(objectType);return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv")).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(objectType.toLowerCase()+"-template.csv",StandardCharsets.UTF_8).build().toString()).body(bytes);}

    @PostMapping(value="/imports",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('system:batch-job:import')")
    public ApiResponse<BatchJob> startImport(@RequestPart String objectType,
            @RequestPart String idempotencyKey,@RequestPart MultipartFile file)throws java.io.IOException{return ApiResponse.success(service.startImport(objectType,idempotencyKey,file.getOriginalFilename(),file.getContentType(),file.getBytes()));}

    @PostMapping("/exports") @PreAuthorize("hasAuthority('system:batch-job:export')")
    public ApiResponse<BatchJob> startExport(@Valid @RequestBody BatchExportRequest request){return ApiResponse.success(service.startExport(request.objectType(),request.idempotencyKey()));}

    @PostMapping("/{id}/retry") @PreAuthorize("hasAuthority('system:batch-job:retry')")
    public ApiResponse<BatchJob> retry(@PathVariable Long id){return ApiResponse.success(service.retry(id));}
}
