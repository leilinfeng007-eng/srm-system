package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisOverdueRepository implements com.srm.system.domain.repository.OverdueTaskRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MybatisOverdueRepository.class);

    private final SysApprovalNodeInstanceMapper nodeMapper;
    private final SysApprovalInstanceMapper instanceMapper;
    private final SysMessageMapper messageMapper;
    private final SysTaskMapper taskMapper;
    private final Clock clock;

    public MybatisOverdueRepository(SysApprovalNodeInstanceMapper nodeMapper,
                                    SysApprovalInstanceMapper instanceMapper,
                                    SysMessageMapper messageMapper,
                                    SysTaskMapper taskMapper,
                                    Clock clock) {
        this.nodeMapper = nodeMapper;
        this.instanceMapper = instanceMapper;
        this.messageMapper = messageMapper;
        this.taskMapper = taskMapper;
        this.clock = clock;
    }

    @Transactional
    public int scanOnce(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SysApprovalNodeInstanceEntity> overdueNodes = nodeMapper.selectList(
                new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                        .eq(SysApprovalNodeInstanceEntity::getStatus, "PENDING")
                        .eq(SysApprovalNodeInstanceEntity::getOverdueNotified, false)
                        .isNotNull(SysApprovalNodeInstanceEntity::getDeadlineAt)
                        .lt(SysApprovalNodeInstanceEntity::getDeadlineAt, now)
                        .last("LIMIT " + batchSize));
        int processed = 0;
        for (SysApprovalNodeInstanceEntity node : overdueNodes) {
            processed += processNode(node, now);
        }
        return processed;
    }

    private int processNode(SysApprovalNodeInstanceEntity node, LocalDateTime now) {
        SysApprovalInstanceEntity instance = instanceMapper.selectById(node.getInstanceId());
        if (instance == null || !"PENDING".equals(instance.getStatus())) {
            return 0;
        }
        node.setOverdueNotified(true);
        node.setUpdatedBy("system");
        int updated = nodeMapper.update(node, new LambdaQueryWrapper<SysApprovalNodeInstanceEntity>()
                .eq(SysApprovalNodeInstanceEntity::getId, node.getId())
                .eq(SysApprovalNodeInstanceEntity::getOverdueNotified, false));
        if (updated == 0) {
            return 0;
        }
        markTasksOverdue(node.getId());
        SysMessageEntity msg = new SysMessageEntity();
        msg.setRecipientId(instance.getSubmittedBy());
        msg.setTitle("审批超期: " + instance.getBusinessSummary());
        msg.setContent("审批节点 '" + node.getNodeName() + "' 已超期。实例: " + instance.getInstanceCode());
        msg.setMessageType("WARNING");
        msg.setStatus("UNREAD");
        msg.setSourceType("APPROVAL_OVERDUE");
        msg.setSourceId(String.valueOf(node.getId()));
        msg.setIdempotencyKey("overdue-" + node.getId());
        msg.setCreatedAt(now);
        try {
            messageMapper.insert(msg);
        } catch (Exception e) {
            LOG.warn("Overdue message insert skipped (dup or error): {}", e.getMessage());
        }
        return 1;
    }

    private void markTasksOverdue(Long nodeId) {
        List<SysTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<SysTaskEntity>()
                .eq(SysTaskEntity::getSourceType, "APPROVAL")
                .eq(SysTaskEntity::getSourceId, String.valueOf(nodeId))
                .eq(SysTaskEntity::getStatus, "PENDING"));
        for (SysTaskEntity task : tasks) {
            task.setSlaStatus("OVERDUE");
            taskMapper.updateById(task);
        }
    }
}
