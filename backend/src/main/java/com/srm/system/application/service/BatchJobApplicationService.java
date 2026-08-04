package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.application.service.MasterDataBatchGateway;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.domain.model.Attachment;
import com.srm.system.domain.model.BatchJob;
import com.srm.system.domain.model.BatchJobError;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.repository.AttachmentRepository;
import com.srm.system.domain.repository.BatchJobRepository;
import com.srm.system.domain.repository.UserRepository;
import com.srm.system.domain.service.AuditRecorder;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class BatchJobApplicationService {
    private final BatchJobRepository jobs;
    private final AttachmentRepository attachments;
    private final MasterDataBatchGateway masterData;
    private final DataScopeAuthorizationService scopes;
    private final UserRepository users;
    private final BatchJobExecutor executor;
    private final AuditRecorder audit;

    public BatchJobApplicationService(BatchJobRepository jobs, AttachmentRepository attachments,
            MasterDataBatchGateway masterData, DataScopeAuthorizationService scopes,
            UserRepository users, BatchJobExecutor executor, AuditRecorder audit) {
        this.jobs=jobs;this.attachments=attachments;this.masterData=masterData;this.scopes=scopes;
        this.users=users;this.executor=executor;this.audit=audit;
    }

    public PageResult<BatchJob> list(int page,int pageSize,String objectType) {
        validatePaging(page,pageSize);
        DataScopeResolution scope=scopes.requireRead("system:batch-job:view","system","OWNER");
        return jobs.findPage(page,pageSize,objectType,scope.isAllScope()?null:username());
    }
    public BatchJob get(Long id){BatchJob job=required(id);requireCovered(job,false,"system:batch-job:view");return job;}
    public List<BatchJobError> errors(Long id){get(id);return jobs.findErrors(id);}

    public byte[] template(String objectType) {
        scopes.requireRead("system:batch-job:view","system","OWNER");
        return CsvCodec.encode(masterData.headers(objectType),List.of());
    }

    public BatchJob startImport(String objectType,String idempotencyKey,String fileName,
                                String mimeType,byte[] content) {
        requireOwnerWrite("system:batch-job:import");
        masterData.headers(objectType);
        String key=key(idempotencyKey);
        var existing=jobs.findByIdempotencyKey(key);
        if(existing.isPresent()){requireCovered(existing.get(),false,"system:batch-job:view");return existing.get();}
        BatchJob job=jobs.create("IMPORT",normalize(objectType),key);
        Attachment input=attachments.store(fileName,mimeType,content,"BATCH_JOB",String.valueOf(job.id()));
        jobs.bindInput(job.id(),input.id());
        audit.record("BATCH_IMPORT_CREATED","BATCH_JOB",String.valueOf(job.id()),"SUCCESS",null,
                "objectType="+job.objectType()+",inputAttachmentId="+input.id(),null);
        executor.executeAsync(job.id());
        return required(job.id());
    }

    public BatchJob startExport(String objectType,String idempotencyKey) {
        requireOwnerWrite("system:batch-job:export");
        masterData.headers(objectType);
        String key=key(idempotencyKey);
        var existing=jobs.findByIdempotencyKey(key);
        if(existing.isPresent()){requireCovered(existing.get(),false,"system:batch-job:view");return existing.get();}
        BatchJob job=jobs.create("EXPORT",normalize(objectType),key);
        audit.record("BATCH_EXPORT_CREATED","BATCH_JOB",String.valueOf(job.id()),"SUCCESS",null,
                "objectType="+job.objectType(),null);
        executor.executeAsync(job.id());
        return job;
    }

    public BatchJob retry(Long id) {
        BatchJob job=required(id);requireCovered(job,true,"system:batch-job:retry");
        if(!jobs.resetFailed(id))throw new BusinessException(ErrorCode.CONFLICT,"Only FAILED jobs can be retried");
        jobs.clearErrors(id);
        audit.record("BATCH_JOB_RETRY","BATCH_JOB",String.valueOf(id),"SUCCESS",
                "status="+job.status(),"status=QUEUED",null);
        executor.executeAsync(id);
        return required(id);
    }

    private void requireOwnerWrite(String permission){DataScopeResolution s=scopes.requireWrite(permission,"system","OWNER");if(!(s.isAllScope()||s.isSelfScope()))throw new BusinessException(ErrorCode.ACCESS_DENIED);}
    private void requireCovered(BatchJob job,boolean write,String permission){DataScopeResolution s=write?scopes.requireWrite(permission,"system","OWNER"):scopes.requireRead(permission,"system","OWNER");Long owner=users.findByUsername(job.createdBy()).map(u->u.id()).orElse(null);if(!(s.isAllScope()||s.coversOwner(owner)))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);}
    private BatchJob required(Long id){return jobs.findById(id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    private String username(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String key(String raw){if(raw==null||raw.isBlank()||raw.length()>80)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Invalid idempotency key");return username()+"|"+raw.trim();}
    private String normalize(String value){return value.trim().toUpperCase(java.util.Locale.ROOT).replace('-','_');}
    private void validatePaging(int p,int s){if(p<1||s<1||s>200)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Invalid paging parameters");}
}
