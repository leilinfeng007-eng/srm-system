package com.srm.system.domain.repository;

public interface ParameterVersionRepository {
    void approveVersion(Long versionId);
    void rejectVersion(Long versionId);
    void withdrawVersion(Long versionId);
}
