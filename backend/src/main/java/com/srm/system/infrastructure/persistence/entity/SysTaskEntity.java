package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_task")
public class SysTaskEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String sourceType; private String sourceId; private String title;
    private Long assigneeId; private String status;
    private String priority; private String businessUrl;
    private String slaStatus; private String resultSummary;
    private String idempotencyKey;
    private LocalDateTime createdAt; private LocalDateTime dueAt;
    private LocalDateTime completedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getSourceType() { return sourceType; } public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; } public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public Long getAssigneeId() { return assigneeId; } public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; } public void setPriority(String priority) { this.priority = priority; }
    public String getBusinessUrl() { return businessUrl; } public void setBusinessUrl(String businessUrl) { this.businessUrl = businessUrl; }
    public String getSlaStatus() { return slaStatus; } public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }
    public String getResultSummary() { return resultSummary; } public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDueAt() { return dueAt; } public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
