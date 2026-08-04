package com.srm.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.infrastructure.persistence.entity.*;
import com.srm.system.infrastructure.persistence.mapper.*;
import com.srm.system.domain.model.BatchJob;
import com.srm.system.domain.model.BatchJobError;
import com.srm.system.domain.repository.BatchJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MybatisBatchJobRepository implements BatchJobRepository {
    private final SysBatchJobMapper jobMapper;
    private final SysBatchJobErrorMapper errorMapper;

    public MybatisBatchJobRepository(SysBatchJobMapper jobMapper, SysBatchJobErrorMapper errorMapper) {
        this.jobMapper = jobMapper;
        this.errorMapper = errorMapper;
    }

    public PageResult<BatchJob> listJobs(int page, int pageSize, String objectType) {
        return findPage(page, pageSize, objectType, null);
    }

    @Override
    public PageResult<BatchJob> findPage(int page, int pageSize, String objectType, String creator) {
        var wrapper = new LambdaQueryWrapper<SysBatchJobEntity>();
        if (objectType != null && !objectType.isEmpty())
            wrapper.eq(SysBatchJobEntity::getObjectType, objectType);
        if (creator != null) wrapper.eq(SysBatchJobEntity::getCreatedBy, creator);
        wrapper.orderByDesc(SysBatchJobEntity::getCreatedAt);
        long total = jobMapper.selectCount(wrapper);
        int offset = (page - 1) * pageSize;
        wrapper.last("LIMIT " + offset + "," + pageSize);
        List<BatchJob> items = jobMapper.selectList(wrapper).stream().map(this::toDomain).toList();
        return PageResult.of(items, page, pageSize, total);
    }

    public BatchJob getJob(Long id) {
        var j = jobMapper.selectById(id);
        if (j == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return toDomain(j);
    }

    public List<BatchJobError> listErrors(Long jobId) {
        return errorMapper.selectList(new LambdaQueryWrapper<SysBatchJobErrorEntity>()
                .eq(SysBatchJobErrorEntity::getJobId, jobId)
                .orderByAsc(SysBatchJobErrorEntity::getRowNumber)).stream()
                .map(this::toDomain).toList();
    }

    @Transactional
    public BatchJob createJob(String jobType, String objectType, String idempotencyKey) {
        return create(jobType, objectType, idempotencyKey);
    }

    @Override
    @Transactional
    public BatchJob create(String jobType, String objectType, String idempotencyKey) {
        var e = new SysBatchJobEntity();
        e.setJobType(jobType); e.setObjectType(objectType);
        e.setStatus("QUEUED"); e.setIdempotencyKey(idempotencyKey);
        e.setCreatedBy(actor()); e.setUpdatedBy(actor());
        jobMapper.insert(e);
        return toDomain(e);
    }

    @Override public Optional<BatchJob> findById(Long id) {
        return Optional.ofNullable(jobMapper.selectById(id)).map(this::toDomain);
    }

    @Override public Optional<BatchJob> findByIdempotencyKey(String key) {
        return Optional.ofNullable(jobMapper.selectOne(new LambdaQueryWrapper<SysBatchJobEntity>()
                .eq(SysBatchJobEntity::getIdempotencyKey, key))).map(this::toDomain);
    }

    @Override @Transactional public void bindInput(Long id, Long attachmentId) {
        var e = required(id); e.setAttachmentId(attachmentId); e.setUpdatedBy(actor());
        jobMapper.updateById(e);
    }

    @Override @Transactional public boolean claim(Long id) {
        return jobMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysBatchJobEntity>()
                .eq(SysBatchJobEntity::getId, id).eq(SysBatchJobEntity::getStatus, "QUEUED")
                .set(SysBatchJobEntity::getStatus, "RUNNING")
                .set(SysBatchJobEntity::getStartedAt, LocalDateTime.now())
                .set(SysBatchJobEntity::getUpdatedBy, actor())) == 1;
    }

    @Override @Transactional public void complete(Long id, String status, int total,
            int success, int fail, Long resultAttachmentId) {
        int changed = jobMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysBatchJobEntity>()
                .eq(SysBatchJobEntity::getId, id).eq(SysBatchJobEntity::getStatus, "RUNNING")
                .set(SysBatchJobEntity::getStatus, status)
                .set(SysBatchJobEntity::getTotalCount, total)
                .set(SysBatchJobEntity::getSuccessCount, success)
                .set(SysBatchJobEntity::getFailCount, fail)
                .set(SysBatchJobEntity::getProgressPercent, 100)
                .set(SysBatchJobEntity::getResultAttachmentId, resultAttachmentId)
                .set(SysBatchJobEntity::getCompletedAt, LocalDateTime.now())
                .set(SysBatchJobEntity::getUpdatedBy, actor()));
        if (changed != 1) throw new BusinessException(ErrorCode.CONFLICT, "Batch job state changed");
    }

    @Override @Transactional public boolean resetFailed(Long id) {
        return jobMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysBatchJobEntity>()
                .eq(SysBatchJobEntity::getId, id).eq(SysBatchJobEntity::getStatus, "FAILED")
                .set(SysBatchJobEntity::getStatus, "QUEUED")
                .set(SysBatchJobEntity::getStartedAt, null)
                .set(SysBatchJobEntity::getCompletedAt, null)
                .set(SysBatchJobEntity::getProgressPercent, 0)
                .set(SysBatchJobEntity::getUpdatedBy, actor())) == 1;
    }

    @Override public List<BatchJobError> findErrors(Long jobId) { return listErrors(jobId); }

    @Override @Transactional public void clearErrors(Long jobId) {
        errorMapper.delete(new LambdaQueryWrapper<SysBatchJobErrorEntity>()
                .eq(SysBatchJobErrorEntity::getJobId, jobId));
    }

    @Transactional
    public void updateProgress(Long jobId, String status, Integer total, Integer success, Integer fail) {
        var e = jobMapper.selectById(jobId);
        if (e == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        e.setStatus(status);
        if (total != null) e.setTotalCount(total);
        if (success != null) e.setSuccessCount(success);
        if (fail != null) e.setFailCount(fail);
        if (total != null && total > 0) {
            int done = (success != null ? success : 0) + (fail != null ? fail : 0);
            e.setProgressPercent(Math.min(100, done * 100 / total));
        }
        if ("SUCCEEDED".equals(status) || "PARTIAL_FAILED".equals(status) || "FAILED".equals(status)) {
            e.setCompletedAt(LocalDateTime.now());
        } else if ("RUNNING".equals(status) && e.getStartedAt() == null) {
            e.setStartedAt(LocalDateTime.now());
        }
        e.setUpdatedBy(actor());
        jobMapper.updateById(e);
    }

    @Transactional
    public void addError(Long jobId, Integer rowNumber, String fieldName, String errorCode, String errorMessage) {
        var e = new SysBatchJobErrorEntity();
        e.setJobId(jobId); e.setRowNumber(rowNumber); e.setFieldName(fieldName);
        e.setErrorCode(errorCode); e.setErrorMessage(errorMessage);
        errorMapper.insert(e);
    }

    private SysBatchJobEntity required(Long id) {
        var entity = jobMapper.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return entity;
    }

    private BatchJob toDomain(SysBatchJobEntity e) {
        return new BatchJob(e.getId(), e.getJobType(), e.getObjectType(), e.getStatus(),
                e.getTotalCount(), e.getSuccessCount(), e.getFailCount(), e.getProgressPercent(),
                e.getAttachmentId(), e.getResultAttachmentId(), e.getIdempotencyKey(),
                e.getStartedAt(), e.getCompletedAt(), e.getCreatedBy(), e.getCreatedAt(), e.getVersion());
    }

    private BatchJobError toDomain(SysBatchJobErrorEntity e) {
        return new BatchJobError(e.getId(), e.getJobId(), e.getRowNumber(), e.getFieldName(),
                e.getErrorCode(), e.getErrorMessage(), e.getCreatedAt());
    }

    private String actor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
