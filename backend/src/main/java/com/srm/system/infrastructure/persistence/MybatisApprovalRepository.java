package com.srm.system.infrastructure.persistence;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.security.infrastructure.persistence.entity.SysUserRoleEntity;
import com.srm.security.infrastructure.persistence.entity.SysUserEntity;
import com.srm.security.infrastructure.persistence.mapper.UserRoleMapper;
import com.srm.security.infrastructure.persistence.mapper.UserAccountMapper;
import com.srm.system.infrastructure.persistence.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisApprovalRepository implements com.srm.system.domain.repository.ApprovalCommandRepository {

    private final SysRoleDataPolicyMapper policyMapper;
    private final SysRoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserAccountMapper userAccountMapper;
    private final ObjectMapper objectMapper;
    private final SysWorkflowDefinitionMapper workflowDefMapper;
    private final SysWorkflowNodeMapper workflowNodeMapper;
    private final SysApprovalInstanceMapper approvalInstanceMapper;
    private final SysApprovalNodeInstanceMapper approvalNodeInstanceMapper;
    private final SysTaskMapper taskMapper;
    private final SysMessageMapper messageMapper;
    private final Clock clock;

    public MybatisApprovalRepository(SysRoleDataPolicyMapper policyMapper, SysRoleMapper roleMapper,
                            UserRoleMapper userRoleMapper, UserAccountMapper userAccountMapper,
                            ObjectMapper objectMapper, Clock clock, SysWorkflowDefinitionMapper workflowDefMapper,
                            SysWorkflowNodeMapper workflowNodeMapper,
                            SysApprovalInstanceMapper approvalInstanceMapper,
                            SysApprovalNodeInstanceMapper approvalNodeInstanceMapper,
                            SysTaskMapper taskMapper, SysMessageMapper messageMapper) {
        this.policyMapper = policyMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userAccountMapper = userAccountMapper;
        this.objectMapper = objectMapper;
        this.workflowDefMapper = workflowDefMapper;
        this.workflowNodeMapper = workflowNodeMapper;
        this.approvalInstanceMapper = approvalInstanceMapper;
        this.approvalNodeInstanceMapper = approvalNodeInstanceMapper;
        this.taskMapper = taskMapper;
        this.messageMapper = messageMapper;
        this.clock = clock;
    }

    @Transactional
    public SysApprovalInstanceEntity submit(String businessType, String businessId, String summary) {
        SrmPrincipal principal = currentPrincipal();
        SysWorkflowDefinitionEntity workflow = findActiveWorkflow(businessType);

        SysApprovalInstanceEntity instance = new SysApprovalInstanceEntity();
        instance.setInstanceCode("APV-" + UUID.randomUUID());
        instance.setWorkflowId(workflow.getId());
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setBusinessSummary(summary);
        instance.setSubmittedBy(principal.userId());
        instance.setSubmittedAt(LocalDateTime.now(clock));
        instance.setStatus("PENDING");
        instance.setSnapshotDefinition(buildSnapshot(workflow));
        instance.setCreatedBy(principal.username());
        instance.setUpdatedBy(principal.username());
        approvalInstanceMapper.insert(instance);

        List<SysWorkflowNodeEntity> nodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowNodeEntity>()
                        .eq(SysWorkflowNodeEntity::getWorkflowId, workflow.getId())
                        .orderByAsc(SysWorkflowNodeEntity::getSortOrder));
        if (nodes.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Published workflow has no approval nodes");
        }
        SysApprovalNodeInstanceEntity firstNode = null;
        for (int index = 0; index < nodes.size(); index++) {
            SysWorkflowNodeEntity node = nodes.get(index);
            List<Long> assigneeSnapshot = resolveDefinitionAssignees(node).stream()
                    .filter(userId -> !userId.equals(principal.userId()))
                    .distinct()
                    .toList();
            if (assigneeSnapshot.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Approval node has no eligible approver after self-approval protection: "
                                + node.getNodeCode());
            }
            SysApprovalNodeInstanceEntity nodeInst = new SysApprovalNodeInstanceEntity();
            nodeInst.setInstanceId(instance.getId());
            nodeInst.setNodeCode(node.getNodeCode());
            nodeInst.setNodeName(node.getNodeName());
            nodeInst.setAssigneeType("SNAPSHOT");
            nodeInst.setAssigneeValue(assigneeSnapshot.stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
            nodeInst.setStatus(index == 0 ? "PENDING" : "WAITING");
            nodeInst.setDurationHours(node.getDurationHours());
            if (index == 0 && node.getDurationHours() != null) {
                nodeInst.setDeadlineAt(LocalDateTime.now(clock).plus(node.getDurationHours(), ChronoUnit.HOURS));
            }
            nodeInst.setCreatedBy(principal.username());
            nodeInst.setUpdatedBy(principal.username());
            approvalNodeInstanceMapper.insert(nodeInst);
            if (index == 0) firstNode = nodeInst;
        }

        createApprovalTasks(instance, firstNode);
        notifyNodeApprovers(instance, firstNode, "有待审批任务",
                "您有新的审批任务: " + summary, "APPROVAL_SUBMITTED",
                "submit-" + instance.getId() + "-" + firstNode.getId());
        return instance;
    }

    @Transactional
    public boolean approve(Long instanceId, String decision) {
        SrmPrincipal principal = currentPrincipal();
        SysApprovalInstanceEntity instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null || !"PENDING".equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval instance is not pending");
        }
        if (instance.getSubmittedBy().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Initiator cannot approve their own request");
        }

        SysApprovalNodeInstanceEntity node = findPendingNodeForApprover(instanceId, principal.userId());
        if (node == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Not an authorized approver for this instance");
        }

        boolean decided = approvalNodeInstanceMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getId, node.getId())
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING")
                        .set(SysApprovalNodeInstanceEntity::getStatus, "APPROVED")
                        .set(SysApprovalNodeInstanceEntity::getDecision, decision)
                        .set(SysApprovalNodeInstanceEntity::getDecidedAt, LocalDateTime.now(clock))
                        .set(SysApprovalNodeInstanceEntity::getActualAssignee, principal.userId())
                        .set(SysApprovalNodeInstanceEntity::getUpdatedBy, principal.username())) > 0;
        if (!decided) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval node was already processed");
        }

        completeTaskForNode(node, "APPROVED");

        SysApprovalNodeInstanceEntity next = approvalNodeInstanceMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "WAITING")
                        .orderByAsc(SysApprovalNodeInstanceEntity::getId)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);

        if (next == null) {
            instance.setStatus("APPROVED");
            instance.setUpdatedBy(principal.username());
            approvalInstanceMapper.updateById(instance);
            completeAllTasks(instanceId, "APPROVED");
            notifyInitiator(instance, "审批通过", "您的申请已通过审批: " + instance.getBusinessSummary(), "APPROVAL_APPROVED", "approve-" + instance.getId());
            return true;
        }
        next.setStatus("PENDING");
        if (next.getDurationHours() != null) {
            next.setDeadlineAt(LocalDateTime.now(clock).plus(next.getDurationHours(), ChronoUnit.HOURS));
        }
        next.setUpdatedBy(principal.username());
        approvalNodeInstanceMapper.updateById(next);
        createApprovalTasks(instance, next);
        notifyNodeApprovers(instance, next, "有待审批任务",
                "您有新的审批任务: " + instance.getBusinessSummary(),
                "APPROVAL_NODE_ACTIVATED", "activate-" + next.getId());
        return false;
    }

    @Transactional
    public void reject(Long instanceId, String reason) {
        SrmPrincipal principal = currentPrincipal();
        SysApprovalInstanceEntity instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null || !"PENDING".equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval instance is not pending");
        }
        if (instance.getSubmittedBy().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Initiator cannot reject their own request");
        }

        SysApprovalNodeInstanceEntity node = findPendingNodeForApprover(instanceId, principal.userId());
        if (node == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Not an authorized approver");
        }

        boolean decided = approvalNodeInstanceMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getId, node.getId())
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING")
                        .set(SysApprovalNodeInstanceEntity::getStatus, "REJECTED")
                        .set(SysApprovalNodeInstanceEntity::getDecision, reason)
                        .set(SysApprovalNodeInstanceEntity::getDecidedAt, LocalDateTime.now(clock))
                        .set(SysApprovalNodeInstanceEntity::getActualAssignee, principal.userId())
                        .set(SysApprovalNodeInstanceEntity::getUpdatedBy, principal.username())) > 0;
        if (!decided) throw new BusinessException(ErrorCode.CONFLICT, "Approval node was already processed");

        cancelRemainingNodes(instanceId, principal.username());

        instance.setStatus("REJECTED");
        instance.setUpdatedBy(principal.username());
        approvalInstanceMapper.updateById(instance);

        completeAllTasks(instanceId, "REJECTED");
        notifyInitiator(instance, "审批驳回", "您的申请被驳回: " + instance.getBusinessSummary(), "APPROVAL_REJECTED", "reject-" + instance.getId());
    }

    @Transactional
    public void withdraw(Long instanceId) {
        SrmPrincipal principal = currentPrincipal();
        SysApprovalInstanceEntity instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null || !"PENDING".equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval instance is not pending");
        }
        if (!instance.getSubmittedBy().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only the initiator can withdraw");
        }
        long completedNodes = approvalNodeInstanceMapper.selectCount(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)
                        .in(SysApprovalNodeInstanceEntity::getStatus, "APPROVED", "REJECTED"));
        if (completedNodes > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "An approval can only be withdrawn before any node is processed");
        }
        notifyApprovers(instance, "申请已撤回", "申请已撤回: " + instance.getBusinessSummary(), "APPROVAL_WITHDRAWN", "withdraw-" + instance.getId());
        cancelRemainingNodes(instanceId, principal.username());
        instance.setStatus("WITHDRAWN");
        instance.setUpdatedBy(principal.username());
        approvalInstanceMapper.updateById(instance);
        completeAllTasks(instanceId, "CANCELLED");
    }

    @Transactional
    public void cancel(Long instanceId) {
        SrmPrincipal principal = currentPrincipal();
        SysApprovalInstanceEntity instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null || !"PENDING".equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Approval instance is not pending");
        }
        notifyApprovers(instance, "审批已取消", "审批已被管理员取消: " + instance.getBusinessSummary(),
                "APPROVAL_CANCELLED", "cancel-" + instance.getId());
        cancelRemainingNodes(instanceId, principal.username());
        instance.setStatus("CANCELLED");
        instance.setUpdatedBy(principal.username());
        approvalInstanceMapper.updateById(instance);
        completeAllTasks(instanceId, "CANCELLED");
        notifyInitiator(instance, "审批已取消", "您的申请已被管理员取消: " + instance.getBusinessSummary(),
                "APPROVAL_CANCELLED", "cancel-initiator-" + instance.getId());
    }

    @Transactional
    public void handleOverdue(Long nodeInstanceId) {
        SysApprovalNodeInstanceEntity node = approvalNodeInstanceMapper.selectById(nodeInstanceId);
        if (node != null && "PENDING".equals(node.getStatus()) && !Boolean.TRUE.equals(node.getOverdueNotified())) {
            int marked = approvalNodeInstanceMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysApprovalNodeInstanceEntity>()
                            .eq(SysApprovalNodeInstanceEntity::getId, nodeInstanceId)
                            .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING")
                            .eq(SysApprovalNodeInstanceEntity::getOverdueNotified, false)
                            .set(SysApprovalNodeInstanceEntity::getOverdueNotified, true)
                            .set(SysApprovalNodeInstanceEntity::getUpdatedBy, "system"));
            if (marked != 1) return;

            SysApprovalInstanceEntity instance = approvalInstanceMapper.selectById(node.getInstanceId());
            if (instance != null) {
                String content = "Approval node '" + node.getNodeName()
                        + "' is overdue. Instance: " + instance.getInstanceCode();
                insertMessage(instance.getSubmittedBy(), "Approval Overdue: " + instance.getBusinessSummary(),
                        content, "APPROVAL_OVERDUE", String.valueOf(node.getId()),
                        "overdue-initiator-" + nodeInstanceId);
                for (Long assigneeId : resolveAssignees(node)) {
                    insertMessage(assigneeId, "待办审批已超期: " + instance.getBusinessSummary(),
                            content, "APPROVAL_OVERDUE", String.valueOf(node.getId()),
                            "overdue-approver-" + nodeInstanceId + "-" + assigneeId);
                }
            }
        }
    }

    private void createApprovalTasks(SysApprovalInstanceEntity instance,
                                     SysApprovalNodeInstanceEntity node) {
        for (Long assigneeId : resolveAssignees(node)) {
                SysTaskEntity task = new SysTaskEntity();
                task.setSourceType("APPROVAL");
                task.setSourceId(String.valueOf(node.getId()));
                task.setTitle(instance.getBusinessSummary() + " - " + node.getNodeName());
                task.setAssigneeId(assigneeId);
                task.setStatus("PENDING");
                task.setPriority("MEDIUM");
                task.setIdempotencyKey("approval-" + node.getId() + "-" + assigneeId);
                if (node.getDeadlineAt() != null) {
                    task.setDueAt(node.getDeadlineAt());
                }
                task.setCreatedAt(LocalDateTime.now(clock));
                taskMapper.insert(task);
        }
    }

    private List<Long> resolveDefinitionAssignees(SysWorkflowNodeEntity node) {
        if ("USER".equals(node.getAssigneeType())) {
            try {
                return List.of(Long.parseLong(node.getAssigneeValue()));
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        } else if ("ROLE".equals(node.getAssigneeType())) {
            Long roleId = roleMapper.findIdByRoleCode(node.getAssigneeValue());
            if (roleId == null) return Collections.emptyList();
            return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>()
                            .eq(SysUserRoleEntity::getRoleId, roleId)
                            .eq(SysUserRoleEntity::getStatus, "ACTIVE")).stream()
                    .map(SysUserRoleEntity::getUserId)
                    .filter(userId -> {
                        var user = userAccountMapper.selectById(userId);
                        return user != null && "ACTIVE".equals(user.getStatus());
                    })
                    .distinct()
                    .collect(Collectors.toList());
        } else if ("POSITION".equals(node.getAssigneeType())) {
            final Long positionId;
            try {
                positionId = Long.parseLong(node.getAssigneeValue());
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
            return userAccountMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                            .eq(SysUserEntity::getMainPositionId, positionId)
                            .eq(SysUserEntity::getStatus, "ACTIVE")).stream()
                    .map(SysUserEntity::getId)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<Long> resolveAssignees(SysApprovalNodeInstanceEntity node) {
        if (!"SNAPSHOT".equals(node.getAssigneeType()) || node.getAssigneeValue() == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(node.getAssigneeValue().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private SysApprovalNodeInstanceEntity findPendingNodeForApprover(Long instanceId, Long userId) {
        List<SysApprovalNodeInstanceEntity> nodes = approvalNodeInstanceMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING")
                        .orderByAsc(SysApprovalNodeInstanceEntity::getId));
        for (SysApprovalNodeInstanceEntity node : nodes) {
            List<Long> assignees = resolveAssignees(node);
            if (assignees.contains(userId)) {
                return node;
            }
        }
        return null;
    }

    private void completeTaskForNode(SysApprovalNodeInstanceEntity node, String result) {
        List<SysTaskEntity> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<SysTaskEntity>()
                        .eq(SysTaskEntity::getSourceId, String.valueOf(node.getId()))
                        .eq(SysTaskEntity::getSourceType, "APPROVAL")
                        .eq(SysTaskEntity::getStatus, "PENDING"));
        for (SysTaskEntity task : tasks) {
            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now(clock));
            task.setResultSummary(result);
            taskMapper.updateById(task);
        }
    }

    private void completeAllTasks(Long instanceId, String result) {
        List<SysApprovalNodeInstanceEntity> nodes = approvalNodeInstanceMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId));
        for (SysApprovalNodeInstanceEntity node : nodes) {
            completeTaskForNode(node, result);
        }
    }

    private SysWorkflowDefinitionEntity findActiveWorkflow(String businessType) {
        List<SysWorkflowDefinitionEntity> workflows = workflowDefMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowDefinitionEntity>()
                        .eq(SysWorkflowDefinitionEntity::getBusinessType, businessType)
                        .eq(SysWorkflowDefinitionEntity::getStatus, "PUBLISHED")
                        .orderByDesc(SysWorkflowDefinitionEntity::getDefinitionVersion));
        if (workflows.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "No published workflow for business type: " + businessType);
        }
        return workflows.get(0);
    }

    private String buildSnapshot(SysWorkflowDefinitionEntity workflow) {
        List<SysWorkflowNodeEntity> nodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowNodeEntity>()
                        .eq(SysWorkflowNodeEntity::getWorkflowId, workflow.getId())
                        .orderByAsc(SysWorkflowNodeEntity::getSortOrder));
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("processCode", workflow.getProcessCode());
            snapshot.put("processName", workflow.getProcessName());
            snapshot.put("version", workflow.getDefinitionVersion());
            snapshot.put("nodes", nodes.stream().map(n -> {
                Map<String, Object> nodeMap = new LinkedHashMap<>();
                nodeMap.put("nodeCode", n.getNodeCode());
                nodeMap.put("nodeName", n.getNodeName());
                nodeMap.put("assigneeType", n.getAssigneeType());
                nodeMap.put("assigneeValue", n.getAssigneeValue());
                return nodeMap;
            }).collect(Collectors.toList()));
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private SrmPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal p) {
            return p;
        }
        throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    private void notifyApprovers(SysApprovalInstanceEntity instance, String title, String content, String sourceType, String idemKey) {
        List<SysApprovalNodeInstanceEntity> nodes = approvalNodeInstanceMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instance.getId())
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING"));
        for (SysApprovalNodeInstanceEntity node : nodes) {
            for (Long assigneeId : resolveAssignees(node)) {
                insertMessage(assigneeId, title, content, sourceType, String.valueOf(instance.getId()), idemKey + "-" + assigneeId);
            }
        }
    }

    private void notifyInitiator(SysApprovalInstanceEntity instance, String title, String content, String sourceType, String idemKey) {
        insertMessage(instance.getSubmittedBy(), title, content, sourceType, String.valueOf(instance.getId()), idemKey);
    }

    private void insertMessage(Long recipientId, String title, String content, String sourceType, String sourceId, String idemKey) {
        if (messageMapper.selectCount(new LambdaQueryWrapper<SysMessageEntity>()
                .eq(SysMessageEntity::getIdempotencyKey, idemKey)) > 0) return;
        SysMessageEntity msg = new SysMessageEntity();
            msg.setRecipientId(recipientId);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setMessageType("INFO");
            msg.setStatus("UNREAD");
            msg.setSourceType(sourceType);
            msg.setSourceId(sourceId);
            msg.setIdempotencyKey(idemKey);
            msg.setCreatedAt(LocalDateTime.now(clock));
        messageMapper.insert(msg);
    }

    private void cancelRemainingNodes(Long instanceId, String operator) {
        List<SysApprovalNodeInstanceEntity> pending = approvalNodeInstanceMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getInstanceId, instanceId)
                        .in(SysApprovalNodeInstanceEntity::getStatus, "PENDING", "WAITING"));
        for (SysApprovalNodeInstanceEntity node : pending) {
            node.setStatus("CANCELLED");
            node.setUpdatedBy(operator);
            approvalNodeInstanceMapper.updateById(node);
        }
    }

    private void notifyNodeApprovers(SysApprovalInstanceEntity instance,
                                     SysApprovalNodeInstanceEntity node,
                                     String title, String content,
                                     String sourceType, String idemKey) {
        for (Long assigneeId : resolveAssignees(node)) {
            insertMessage(assigneeId, title, content, sourceType,
                    String.valueOf(instance.getId()), idemKey + "-" + assigneeId);
        }
    }
}
