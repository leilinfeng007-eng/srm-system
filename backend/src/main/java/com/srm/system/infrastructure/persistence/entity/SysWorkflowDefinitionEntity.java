package com.srm.system.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("sys_workflow_definition")
public class SysWorkflowDefinitionEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String processCode;
    private String processName;
    private String businessType;
    private Integer definitionVersion;
    private String status;
    private String description;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    @Version private Long version;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getProcessCode() { return processCode; } public void setProcessCode(String processCode) { this.processCode = processCode; }
    public String getProcessName() { return processName; } public void setProcessName(String processName) { this.processName = processName; }
    public String getBusinessType() { return businessType; } public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Integer getDefinitionVersion() { return definitionVersion; } public void setDefinitionVersion(Integer definitionVersion) { this.definitionVersion = definitionVersion; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; } public void setVersion(Long version) { this.version = version; }
}
