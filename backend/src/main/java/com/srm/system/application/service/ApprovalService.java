package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.domain.repository.ApprovalCommandRepository;
import com.srm.system.domain.repository.ApprovalContextRepository;
import com.srm.system.domain.service.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private final ApprovalCommandRepository repository;
    private final ApprovalContextRepository queryRepo;
    private final ApprovalCallbackRegistry callbackRegistry;
    private final AuditRecorder audit;

    public ApprovalService(ApprovalCommandRepository repository,
                           ApprovalContextRepository queryRepo,
                           ApprovalCallbackRegistry callbackRegistry,
                           AuditRecorder audit) {
        this.repository = repository;
        this.queryRepo = queryRepo;
        this.callbackRegistry = callbackRegistry;
        this.audit = audit;
    }

    @Transactional
    public Object submit(String businessType, String businessId, String summary) {
        if (callbackRegistry.find(businessType) == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No approval callback registered for business type: " + businessType);
        }
        Object result = repository.submit(businessType, businessId, summary);
        audit.record("APPROVAL_SUBMITTED", businessType, businessId, "SUCCESS",
                null, "status=IN_APPROVAL", null);
        return result;
    }

    @Transactional
    public void approve(Long instanceId, String decision) {
        boolean finalNodeApproved = repository.approve(instanceId, decision);
        if (finalNodeApproved) {
            fireApproved(instanceId);
        }
        audit.record("APPROVAL_NODE_APPROVED", "APPROVAL_INSTANCE", String.valueOf(instanceId),
                "SUCCESS", null, finalNodeApproved ? "status=APPROVED" : "nextNode=ACTIVE", null);
    }

    @Transactional
    public void reject(Long instanceId, String reason) {
        repository.reject(instanceId, reason);
        fireRejected(instanceId);
        audit.record("APPROVAL_REJECTED", "APPROVAL_INSTANCE", String.valueOf(instanceId),
                "SUCCESS", null, "status=REJECTED", reason);
    }

    @Transactional
    public void withdraw(Long instanceId) {
        repository.withdraw(instanceId);
        var ctx = queryRepo.findApprovalContext(instanceId);
        var callback = callbackRegistry.find(ctx.businessType());
        if (callback != null) callback.onWithdrawn(ctx.businessId());
        audit.record("APPROVAL_WITHDRAWN", "APPROVAL_INSTANCE", String.valueOf(instanceId),
                "SUCCESS", null, "status=WITHDRAWN", null);
    }

    @Transactional
    public void cancel(Long instanceId) {
        repository.cancel(instanceId);
        var ctx = queryRepo.findApprovalContext(instanceId);
        var callback = callbackRegistry.find(ctx.businessType());
        if (callback != null) callback.onCancelled(ctx.businessId());
        audit.record("APPROVAL_CANCELLED", "APPROVAL_INSTANCE", String.valueOf(instanceId),
                "SUCCESS", null, "status=CANCELLED", null);
    }

    @Transactional
    public void handleOverdue(Long nodeInstanceId) {
        repository.handleOverdue(nodeInstanceId);
        audit.record("APPROVAL_NODE_OVERDUE", "APPROVAL_NODE", String.valueOf(nodeInstanceId),
                "SUCCESS", null, "overdueHandled=true", null);
    }

    private void fireApproved(Long instanceId) {
        var ctx = queryRepo.findApprovalContext(instanceId);
        var callback = callbackRegistry.find(ctx.businessType());
        if (callback != null) {
            callback.onApproved(ctx.businessId());
        }
    }

    private void fireRejected(Long instanceId) {
        var ctx = queryRepo.findApprovalContext(instanceId);
        var callback = callbackRegistry.find(ctx.businessType());
        if (callback != null) {
            callback.onRejected(ctx.businessId());
        }
    }
}
