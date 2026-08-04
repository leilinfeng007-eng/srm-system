package com.srm.system.domain.service;

public interface AuditRecorder {
    void record(String actionCode, String targetType, String targetId, String resultCode,
                String beforeSummary, String afterSummary, String reason);
}
