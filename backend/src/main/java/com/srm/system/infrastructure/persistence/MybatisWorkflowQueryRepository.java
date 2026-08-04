package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisWorkflowQueryRepository implements com.srm.system.domain.repository.ApprovalContextRepository {

    private final SysWorkflowDefinitionMapper workflowDefMapper;
    private final SysWorkflowNodeMapper workflowNodeMapper;
    private final SysApprovalInstanceMapper approvalInstanceMapper;
    private final SysApprovalNodeInstanceMapper approvalNodeInstanceMapper;
    private final SysTaskMapper taskMapper;
    private final SysMessageMapper messageMapper;
    private final Clock clock;

    public MybatisWorkflowQueryRepository(SysWorkflowDefinitionMapper w, SysWorkflowNodeMapper wn,
                                           SysApprovalInstanceMapper ai, SysApprovalNodeInstanceMapper ani,
                                           SysTaskMapper t, SysMessageMapper m, Clock clock) {
        this.workflowDefMapper = w; this.workflowNodeMapper = wn;
        this.approvalInstanceMapper = ai; this.approvalNodeInstanceMapper = ani;
        this.taskMapper = t; this.messageMapper = m; this.clock = clock;
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    // === Workflow definitions ===
    public PageResult<SysWorkflowDefinitionEntity> listWorkflows(int page, int pageSize, String status) {
        LambdaQueryWrapper<SysWorkflowDefinitionEntity> cw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysWorkflowDefinitionEntity> lw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) { cw.eq(SysWorkflowDefinitionEntity::getStatus, status); lw.eq(SysWorkflowDefinitionEntity::getStatus, status); }
        long total = workflowDefMapper.selectCount(cw);
        int offset = (page - 1) * pageSize;
        List<SysWorkflowDefinitionEntity> items = workflowDefMapper.selectList(lw.orderByDesc(SysWorkflowDefinitionEntity::getUpdatedAt).last("LIMIT " + offset + "," + pageSize));
        return PageResult.of(items, page, pageSize, total);
    }

    public SysWorkflowDefinitionEntity getWorkflow(Long id) {
        var e = workflowDefMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return e;
    }

    public List<SysWorkflowNodeEntity> listWorkflowNodes(Long workflowId) {
        return workflowNodeMapper.selectList(new LambdaQueryWrapper<SysWorkflowNodeEntity>()
                .eq(SysWorkflowNodeEntity::getWorkflowId, workflowId)
                .orderByAsc(SysWorkflowNodeEntity::getSortOrder));
    }

    @Transactional
    public SysWorkflowDefinitionEntity createWorkflow(String processCode, String processName, String businessType, String description, List<SysWorkflowNodeEntity> nodes) {
        List<SysWorkflowDefinitionEntity> existingVersions = workflowDefMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowDefinitionEntity>()
                        .eq(SysWorkflowDefinitionEntity::getProcessCode, processCode)
                        .orderByDesc(SysWorkflowDefinitionEntity::getDefinitionVersion));
        if (existingVersions.stream().anyMatch(version -> "DRAFT".equals(version.getStatus()))) {
            throw new BusinessException(ErrorCode.CONFLICT, "A draft workflow with this code already exists");
        }
        if (!existingVersions.isEmpty()
                && !java.util.Objects.equals(existingVersions.get(0).getBusinessType(), businessType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Workflow business type cannot change between versions");
        }
        int nextVersion = existingVersions.stream()
                .map(SysWorkflowDefinitionEntity::getDefinitionVersion)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
        var def = new SysWorkflowDefinitionEntity();
        def.setProcessCode(processCode); def.setProcessName(processName); def.setBusinessType(businessType);
        def.setDefinitionVersion(nextVersion); def.setStatus("DRAFT"); def.setDescription(description);
        def.setCreatedBy(actor()); def.setUpdatedBy(actor());
        def.setVersion(0L);
        workflowDefMapper.insert(def);
        saveNodes(def.getId(), nodes);
        return def;
    }

    @Transactional
    public void updateWorkflow(Long id, String processName, String description, List<SysWorkflowNodeEntity> nodes) {
        var def = workflowDefMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if ("PUBLISHED".equals(def.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Published workflow cannot be modified");
        }
        if (processName != null) def.setProcessName(processName);
        if (description != null) def.setDescription(description);
        def.setUpdatedBy(actor());
        workflowDefMapper.updateById(def);
        if (nodes != null) {
            workflowNodeMapper.delete(new LambdaQueryWrapper<SysWorkflowNodeEntity>()
                    .eq(SysWorkflowNodeEntity::getWorkflowId, id));
            saveNodes(id, nodes);
        }
    }

    @Transactional
    public void publishWorkflow(Long id) {
        var def = workflowDefMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if ("PUBLISHED".equals(def.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Workflow already published");
        }
        long nodeCount = workflowNodeMapper.selectCount(new LambdaQueryWrapper<SysWorkflowNodeEntity>()
                .eq(SysWorkflowNodeEntity::getWorkflowId, id));
        if (nodeCount == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot publish workflow without nodes");
        }
        def.setStatus("PUBLISHED");
        def.setUpdatedBy(actor());
        workflowDefMapper.updateById(def);
        List<SysWorkflowDefinitionEntity> olderPublished = workflowDefMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowDefinitionEntity>()
                        .eq(SysWorkflowDefinitionEntity::getBusinessType, def.getBusinessType())
                        .eq(SysWorkflowDefinitionEntity::getStatus, "PUBLISHED")
                        .ne(SysWorkflowDefinitionEntity::getId, def.getId()));
        for (SysWorkflowDefinitionEntity older : olderPublished) {
            older.setStatus("RETIRED");
            older.setUpdatedBy(actor());
            workflowDefMapper.updateById(older);
        }
    }

    @Transactional
    public void retireWorkflow(Long id) {
        var def = workflowDefMapper.selectById(id);
        if (def == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        def.setStatus("RETIRED");
        def.setUpdatedBy(actor());
        workflowDefMapper.updateById(def);
    }

    private void saveNodes(Long workflowId, List<SysWorkflowNodeEntity> nodes) {
        if (nodes == null) return;
        for (SysWorkflowNodeEntity n : nodes) {
            n.setId(null);
            n.setWorkflowId(workflowId);
            n.setCreatedBy(actor());
            n.setUpdatedBy(actor());
            workflowNodeMapper.insert(n);
        }
    }

    // === Approval instances ===
    public PageResult<SysApprovalInstanceEntity> listApprovals(int page, int pageSize, String status, Long userId) {
        LambdaQueryWrapper<SysApprovalInstanceEntity> cw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysApprovalInstanceEntity> lw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) { cw.eq(SysApprovalInstanceEntity::getStatus, status); lw.eq(SysApprovalInstanceEntity::getStatus, status); }
        if (userId != null) { cw.eq(SysApprovalInstanceEntity::getSubmittedBy, userId); lw.eq(SysApprovalInstanceEntity::getSubmittedBy, userId); }
        long total = approvalInstanceMapper.selectCount(cw);
        int offset = (page - 1) * pageSize;
        List<SysApprovalInstanceEntity> items = approvalInstanceMapper.selectList(lw.orderByDesc(SysApprovalInstanceEntity::getSubmittedAt).last("LIMIT " + offset + "," + pageSize));
        return PageResult.of(items, page, pageSize, total);
    }

    public SysApprovalInstanceEntity getApproval(Long id) {
        var e = approvalInstanceMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return e;
    }

    public List<SysApprovalNodeInstanceEntity> listApprovalNodes(Long instanceId) {
        return approvalNodeInstanceMapper.selectList(new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId));
    }

    public void requireApprovalVisible(Long instanceId, Long userId) {
        SysApprovalInstanceEntity instance = getApproval(instanceId);
        if (userId != null && userId.equals(instance.getSubmittedBy())) return;
        boolean assigned = listApprovalNodes(instanceId).stream()
                .anyMatch(node -> snapshotContains(node.getAssigneeValue(), userId));
        if (!assigned) throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    public Long approvalInstanceIdForTask(SysTaskEntity task) {
        if (!"APPROVAL".equals(task.getSourceType()) || task.getSourceId() == null) return null;
        try {
            var node = approvalNodeInstanceMapper.selectById(Long.valueOf(task.getSourceId()));
            return node == null ? null : node.getInstanceId();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean snapshotContains(String snapshot, Long userId) {
        if (snapshot == null || userId == null) return false;
        String expected = String.valueOf(userId);
        return java.util.Arrays.stream(snapshot.split(","))
                .map(String::trim).anyMatch(expected::equals);
    }

    public record ApprovalContext(String businessType, String businessId) {}

    public ApprovalContext getApprovalContext(Long instanceId) {
        var e = approvalInstanceMapper.selectById(instanceId);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return new ApprovalContext(e.getBusinessType(), e.getBusinessId());
    }

    @Override
    public com.srm.system.domain.repository.ApprovalContextRepository.ApprovalContext findApprovalContext(
            Long instanceId) {
        ApprovalContext context = getApprovalContext(instanceId);
        return new com.srm.system.domain.repository.ApprovalContextRepository.ApprovalContext(
                context.businessType(), context.businessId());
    }

    // === Tasks ===
    public PageResult<SysTaskEntity> listMyTasks(int page, int pageSize, Long assigneeId, String status) {
        LambdaQueryWrapper<SysTaskEntity> cw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysTaskEntity> lw = new LambdaQueryWrapper<>();
        cw.eq(SysTaskEntity::getAssigneeId, assigneeId);
        lw.eq(SysTaskEntity::getAssigneeId, assigneeId);
        if (status != null && !status.isBlank()) { cw.eq(SysTaskEntity::getStatus, status); lw.eq(SysTaskEntity::getStatus, status); }
        long total = taskMapper.selectCount(cw);
        int offset = (page - 1) * pageSize;
        List<SysTaskEntity> items = taskMapper.selectList(lw.orderByDesc(SysTaskEntity::getCreatedAt).last("LIMIT " + offset + "," + pageSize));
        return PageResult.of(items, page, pageSize, total);
    }

    public long countOverdueTasks(Long assigneeId) {
        return taskMapper.selectCount(new LambdaQueryWrapper<SysTaskEntity>()
                .eq(SysTaskEntity::getAssigneeId, assigneeId)
                .eq(SysTaskEntity::getStatus, "PENDING")
                .lt(SysTaskEntity::getDueAt, LocalDateTime.now(clock)));
    }

    // === Messages ===
    public PageResult<SysMessageEntity> listMyMessages(int page, int pageSize, Long recipientId, String status) {
        LambdaQueryWrapper<SysMessageEntity> cw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<SysMessageEntity> lw = new LambdaQueryWrapper<>();
        cw.eq(SysMessageEntity::getRecipientId, recipientId);
        lw.eq(SysMessageEntity::getRecipientId, recipientId);
        if (status != null && !status.isBlank()) { cw.eq(SysMessageEntity::getStatus, status); lw.eq(SysMessageEntity::getStatus, status); }
        long total = messageMapper.selectCount(cw);
        int offset = (page - 1) * pageSize;
        List<SysMessageEntity> items = messageMapper.selectList(lw.orderByDesc(SysMessageEntity::getCreatedAt).last("LIMIT " + offset + "," + pageSize));
        return PageResult.of(items, page, pageSize, total);
    }

    public long countUnreadMessages(Long recipientId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getRecipientId, recipientId)
                .eq(SysMessageEntity::getStatus, "UNREAD"));
    }

    @Transactional
    public void markMessageRead(Long id, Long recipientId) {
        var m = messageMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!m.getRecipientId().equals(recipientId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Cannot read another user's message");
        }
        m.setStatus("READ");
        m.setReadAt(LocalDateTime.now(clock));
        messageMapper.updateById(m);
    }

    @Transactional
    public int markAllMessagesRead(Long recipientId) {
        var list = messageMapper.selectList(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getRecipientId, recipientId)
                .eq(SysMessageEntity::getStatus, "UNREAD"));
        for (SysMessageEntity m : list) {
            m.setStatus("READ");
            m.setReadAt(LocalDateTime.now(clock));
            messageMapper.updateById(m);
        }
        return list.size();
    }
}
