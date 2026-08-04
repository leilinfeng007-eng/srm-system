package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_workflow_node")
public class SysWorkflowNodeEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long workflowId; private String nodeCode; private String nodeName;
    private String nodeType; private Integer sortOrder;
    private String assigneeType; private String assigneeValue;
    private Integer durationHours;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getWorkflowId() { return workflowId; } public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    public String getNodeCode() { return nodeCode; } public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getNodeName() { return nodeName; } public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeType() { return nodeType; } public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Integer getSortOrder() { return sortOrder; } public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getAssigneeType() { return assigneeType; } public void setAssigneeType(String assigneeType) { this.assigneeType = assigneeType; }
    public String getAssigneeValue() { return assigneeValue; } public void setAssigneeValue(String assigneeValue) { this.assigneeValue = assigneeValue; }
    public Integer getDurationHours() { return durationHours; } public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
