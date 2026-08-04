package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_batch_job")
public class SysBatchJobEntity {
    @TableId(type=IdType.AUTO) private Long id;
    private String jobType; private String objectType; private String status;
    private Integer totalCount; private Integer successCount; private Integer failCount;
    private Integer progressPercent; private Long attachmentId; private Long resultAttachmentId;
    private String idempotencyKey; private LocalDateTime startedAt; private LocalDateTime completedAt;
    private String createdBy; private LocalDateTime createdAt; private String updatedBy; private LocalDateTime updatedAt;
    private Long version;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getJobType(){return jobType;} public void setJobType(String s){this.jobType=s;}
    public String getObjectType(){return objectType;} public void setObjectType(String s){this.objectType=s;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public Integer getTotalCount(){return totalCount;} public void setTotalCount(Integer v){this.totalCount=v;}
    public Integer getSuccessCount(){return successCount;} public void setSuccessCount(Integer v){this.successCount=v;}
    public Integer getFailCount(){return failCount;} public void setFailCount(Integer v){this.failCount=v;}
    public Integer getProgressPercent(){return progressPercent;} public void setProgressPercent(Integer v){this.progressPercent=v;}
    public Long getAttachmentId(){return attachmentId;} public void setAttachmentId(Long v){this.attachmentId=v;}
    public Long getResultAttachmentId(){return resultAttachmentId;} public void setResultAttachmentId(Long v){this.resultAttachmentId=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String s){this.idempotencyKey=s;}
    public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime t){this.startedAt=t;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime t){this.completedAt=t;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String s){this.createdBy=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String s){this.updatedBy=s;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime t){this.updatedAt=t;}
    public Long getVersion(){return version;} public void setVersion(Long v){this.version=v;}
}
