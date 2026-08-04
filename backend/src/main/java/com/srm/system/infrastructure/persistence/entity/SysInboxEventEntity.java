package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_inbox_event")
public class SysInboxEventEntity {
    @TableId(type=IdType.AUTO) private Long id;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    private String eventId;
    private String sourceSystem;
    private String objectType;
    private String objectId;
    private Long objectVersion;
    private String payloadSummary;
    private String status;
    private String resultMessage;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime processingStartedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    public String getEventId(){return eventId;} public void setEventId(String v){eventId=v;}
    public String getSourceSystem(){return sourceSystem;} public void setSourceSystem(String v){sourceSystem=v;}
    public String getObjectType(){return objectType;} public void setObjectType(String v){objectType=v;}
    public String getObjectId(){return objectId;} public void setObjectId(String v){objectId=v;}
    public Long getObjectVersion(){return objectVersion;} public void setObjectVersion(Long v){objectVersion=v;}
    public String getPayloadSummary(){return payloadSummary;} public void setPayloadSummary(String v){payloadSummary=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getResultMessage(){return resultMessage;} public void setResultMessage(String v){resultMessage=v;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer v){attemptCount=v;}
    public Integer getMaxAttempts(){return maxAttempts;} public void setMaxAttempts(Integer v){maxAttempts=v;}
    public LocalDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(LocalDateTime v){nextRetryAt=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public LocalDateTime getProcessingStartedAt(){return processingStartedAt;} public void setProcessingStartedAt(LocalDateTime v){processingStartedAt=v;}
    public LocalDateTime getReceivedAt(){return receivedAt;} public void setReceivedAt(LocalDateTime v){receivedAt=v;}
    public LocalDateTime getProcessedAt(){return processedAt;} public void setProcessedAt(LocalDateTime v){processedAt=v;}
}
