package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_integration_attempt")
public class SysIntegrationAttemptEntity {
    @TableId(type=IdType.AUTO) private Long id;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    private Long jobId;
    private Integer attemptNumber;
    private String status;
    private String errorMessage;
    private String traceId;
    private LocalDateTime attemptedAt;
    public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
    public Integer getAttemptNumber(){return attemptNumber;} public void setAttemptNumber(Integer v){attemptNumber=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
    public String getTraceId(){return traceId;} public void setTraceId(String v){traceId=v;}
    public LocalDateTime getAttemptedAt(){return attemptedAt;} public void setAttemptedAt(LocalDateTime v){attemptedAt=v;}
}
