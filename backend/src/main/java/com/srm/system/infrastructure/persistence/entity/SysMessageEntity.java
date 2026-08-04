package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_message")
public class SysMessageEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long recipientId; private String title; private String content;
    private String messageType; private String status;
    private String sourceType; private String sourceId;
    private String businessUrl; private String idempotencyKey;
    private LocalDateTime readAt; private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getRecipientId() { return recipientId; } public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public String getMessageType() { return messageType; } public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getSourceType() { return sourceType; } public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; } public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getBusinessUrl() { return businessUrl; } public void setBusinessUrl(String businessUrl) { this.businessUrl = businessUrl; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getReadAt() { return readAt; } public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
