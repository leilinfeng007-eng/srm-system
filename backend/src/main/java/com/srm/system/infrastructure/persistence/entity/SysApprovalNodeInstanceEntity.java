package com.srm.system.infrastructure.persistence.entity;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("sys_approval_node_instance")
public class SysApprovalNodeInstanceEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private Long instanceId; private String nodeCode; private String nodeName;
    private String assigneeType; private String assigneeValue;
    private Long actualAssignee; private String status;
    private String decision; private LocalDateTime decidedAt;
    private Integer durationHours; private LocalDateTime deadlineAt;
    private Boolean overdueNotified;
    private String createdBy; private LocalDateTime createdAt;
    private String updatedBy; private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getInstanceId() { return instanceId; } public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
    public String getNodeCode() { return nodeCode; } public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getNodeName() { return nodeName; } public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getAssigneeType() { return assigneeType; } public void setAssigneeType(String assigneeType) { this.assigneeType = assigneeType; }
    public String getAssigneeValue() { return assigneeValue; } public void setAssigneeValue(String assigneeValue) { this.assigneeValue = assigneeValue; }
    public Long getActualAssignee() { return actualAssignee; } public void setActualAssignee(Long actualAssignee) { this.actualAssignee = actualAssignee; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getDecision() { return decision; } public void setDecision(String decision) { this.decision = decision; }
    public LocalDateTime getDecidedAt() { return decidedAt; } public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public Integer getDurationHours() { return durationHours; } public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; } public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public Boolean getOverdueNotified() { return overdueNotified; } public void setOverdueNotified(Boolean overdueNotified) { this.overdueNotified = overdueNotified; }
    public String getCreatedBy() { return createdBy; } public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
