package com.srm.system.domain.repository;

import com.srm.common.api.PageResult;
import com.srm.system.domain.model.BatchJob;
import com.srm.system.domain.model.BatchJobError;
import java.util.List;
import java.util.Optional;

public interface BatchJobRepository {
    PageResult<BatchJob> findPage(int page, int pageSize, String objectType, String creator);
    Optional<BatchJob> findById(Long id);
    Optional<BatchJob> findByIdempotencyKey(String idempotencyKey);
    BatchJob create(String jobType, String objectType, String idempotencyKey);
    void bindInput(Long jobId, Long attachmentId);
    boolean claim(Long jobId);
    void complete(Long jobId, String status, int total, int success, int fail,
                  Long resultAttachmentId);
    boolean resetFailed(Long jobId);
    List<BatchJobError> findErrors(Long jobId);
    void clearErrors(Long jobId);
    void addError(Long jobId, Integer rowNumber, String fieldName,
                  String errorCode, String errorMessage);
}
