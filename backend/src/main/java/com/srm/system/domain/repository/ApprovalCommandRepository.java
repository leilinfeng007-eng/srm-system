package com.srm.system.domain.repository;

public interface ApprovalCommandRepository {
    Object submit(String businessType, String businessId, String summary);
    boolean approve(Long instanceId, String decision);
    void reject(Long instanceId, String reason);
    void withdraw(Long instanceId);
    void cancel(Long instanceId);
    void handleOverdue(Long nodeInstanceId);
}
