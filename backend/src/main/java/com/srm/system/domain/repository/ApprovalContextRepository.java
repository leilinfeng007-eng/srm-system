package com.srm.system.domain.repository;

public interface ApprovalContextRepository {
    record ApprovalContext(String businessType, String businessId) {}
    ApprovalContext findApprovalContext(Long instanceId);
}
