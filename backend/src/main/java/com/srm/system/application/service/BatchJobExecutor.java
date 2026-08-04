package com.srm.system.application.service;

import com.srm.masterdata.application.service.MasterDataBatchGateway;
import com.srm.system.domain.model.BatchJob;
import com.srm.system.domain.repository.AttachmentRepository;
import com.srm.system.domain.repository.BatchJobRepository;
import com.srm.system.domain.service.AuditRecorder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class BatchJobExecutor {
    private final BatchJobRepository jobs;
    private final AttachmentRepository attachments;
    private final MasterDataBatchGateway masterData;
    private final AuditRecorder audit;

    public BatchJobExecutor(BatchJobRepository jobs,AttachmentRepository attachments,
            MasterDataBatchGateway masterData,AuditRecorder audit){this.jobs=jobs;this.attachments=attachments;this.masterData=masterData;this.audit=audit;}

    @Async("batchTaskExecutor")
    public void executeAsync(Long jobId){execute(jobId);}

    public void execute(Long jobId){
        if(!jobs.claim(jobId))return;
        jobs.clearErrors(jobId);
        BatchJob job=jobs.findById(jobId).orElseThrow();
        try{
            if("IMPORT".equals(job.jobType()))runImport(job);else if("EXPORT".equals(job.jobType()))runExport(job);
            else throw new IllegalArgumentException("Unsupported job type: "+job.jobType());
        }catch(RuntimeException failure){
            String message=safe(failure);jobs.addError(jobId,null,null,"JOB_FAILED",message);
            Long result=storeErrors(jobId,List.of(List.of("","","JOB_FAILED",message)));
            jobs.complete(jobId,"FAILED",0,0,1,result);
            audit.record("BATCH_JOB_EXECUTED","BATCH_JOB",String.valueOf(jobId),"FAILED","status=RUNNING","status=FAILED",message);
        }
    }

    private void runImport(BatchJob job){
        if(job.attachmentId()==null)throw new IllegalStateException("Import attachment is missing");
        CsvCodec.CsvData csv=CsvCodec.parse(attachments.loadContent(job.attachmentId()));
        List<String> expected=masterData.headers(job.objectType());
        if(!csv.headers().equals(expected))throw new IllegalArgumentException("CSV header does not match template: "+String.join(",",expected));
        int success=0;List<List<String>> errors=new ArrayList<>();
        for(int i=0;i<csv.rows().size();i++){
            List<String> row=csv.rows().get(i);int rowNumber=i+2;
            if(row.size()!=expected.size()){String msg="Column count does not match template";jobs.addError(job.id(),rowNumber,null,"COLUMN_COUNT",msg);errors.add(List.of(String.valueOf(rowNumber),"","COLUMN_COUNT",msg));continue;}
            Map<String,String> values=new LinkedHashMap<>();for(int c=0;c<expected.size();c++)values.put(expected.get(c),row.get(c));
            try{masterData.importRow(job.objectType(),values);success++;}
            catch(RuntimeException failure){String msg=safe(failure);jobs.addError(job.id(),rowNumber,null,"ROW_VALIDATION",msg);errors.add(List.of(String.valueOf(rowNumber),"","ROW_VALIDATION",msg));}
        }
        Long result=errors.isEmpty()?null:storeErrors(job.id(),errors);
        String status=errors.isEmpty()?"SUCCEEDED":success==0?"FAILED":"PARTIAL_FAILED";
        jobs.complete(job.id(),status,csv.rows().size(),success,errors.size(),result);
        audit.record("BATCH_IMPORT_EXECUTED","BATCH_JOB",String.valueOf(job.id()),"SUCCESS","status=RUNNING","status="+status+",success="+success+",fail="+errors.size(),null);
    }

    private void runExport(BatchJob job){
        List<List<String>> rows=new ArrayList<>();List<String> headers=masterData.headers(job.objectType());long total=0;
        for(int page=1;;page++){
            var result=masterData.exportPage(job.objectType(),page,200);total=result.total();rows.addAll(result.rows());if(rows.size()>=total||result.rows().isEmpty())break;
        }
        byte[] csv=CsvCodec.encode(headers,rows);
        Long result=attachments.store(job.objectType().toLowerCase(java.util.Locale.ROOT)+"-export.csv","text/csv",csv,"BATCH_JOB",String.valueOf(job.id())).id();
        jobs.complete(job.id(),"SUCCEEDED",Math.toIntExact(total),Math.toIntExact(total),0,result);
        audit.record("BATCH_EXPORT_EXECUTED","BATCH_JOB",String.valueOf(job.id()),"SUCCESS","status=RUNNING","status=SUCCEEDED,total="+total,null);
    }

    private Long storeErrors(Long jobId,List<List<String>> errors){byte[] csv=CsvCodec.encode(List.of("rowNumber","fieldName","errorCode","errorMessage"),errors);return attachments.store("batch-"+jobId+"-errors.csv","text/csv",csv,"BATCH_JOB",String.valueOf(jobId)).id();}
    private String safe(Throwable failure){String value=failure.getMessage();if(value==null||value.isBlank())value=failure.getClass().getSimpleName();return value.length()>500?value.substring(0,500):value;}
}
