package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_outbox_event")
public class SysOutboxEventEntity {
    @TableId(type=IdType.AUTO) private Long id;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    private String eventId;
    private String objectType;
    private String objectId;
    private Long objectVersion;
    private String eventType;
    private String payloadSummary;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private String traceId;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    public String getEventId(){return eventId;} public void setEventId(String v){eventId=v;}
    public String getObjectType(){return objectType;} public void setObjectType(String v){objectType=v;}
    public String getObjectId(){return objectId;} public void setObjectId(String v){objectId=v;}
    public Long getObjectVersion(){return objectVersion;} public void setObjectVersion(Long v){objectVersion=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public String getPayloadSummary(){return payloadSummary;} public void setPayloadSummary(String v){payloadSummary=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Integer getAttemptCount(){return attemptCount;} public void setAttemptCount(Integer v){attemptCount=v;}
    public Integer getMaxAttempts(){return maxAttempts;} public void setMaxAttempts(Integer v){maxAttempts=v;}
    public LocalDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(LocalDateTime v){nextRetryAt=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public String getTraceId(){return traceId;} public void setTraceId(String v){traceId=v;}
    public LocalDateTime getOccurredAt(){return occurredAt;} public void setOccurredAt(LocalDateTime v){occurredAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
