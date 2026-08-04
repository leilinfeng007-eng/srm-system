package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_approval_instance")
public class SysApprovalInstanceEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String instanceCode; private Long workflowId;
    private String businessType; private String businessId;
    private String businessSummary; private Long submittedBy;
    private LocalDateTime submittedAt; private String status;
    private String snapshotDefinition;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getInstanceCode() { return instanceCode; } public void setInstanceCode(String instanceCode) { this.instanceCode = instanceCode; }
    public Long getWorkflowId() { return workflowId; } public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    public String getBusinessType() { return businessType; } public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessId() { return businessId; } public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getBusinessSummary() { return businessSummary; } public void setBusinessSummary(String businessSummary) { this.businessSummary = businessSummary; }
    public Long getSubmittedBy() { return submittedBy; } public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getSnapshotDefinition() { return snapshotDefinition; } public void setSnapshotDefinition(String snapshotDefinition) { this.snapshotDefinition = snapshotDefinition; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
}
