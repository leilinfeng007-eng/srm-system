package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.api.request.CreateDepartmentRequest;
import com.srm.system.api.request.UpdateDepartmentRequest;
import com.srm.system.api.response.DepartmentResponse;
import com.srm.system.domain.model.Department;
import com.srm.system.domain.repository.DepartmentRepository;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.service.AuditRecorder;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentApplicationService {

    private final DepartmentRepository departmentRepository;
    private final DataScopeAuthorizationService dataScope;
    private final AuditRecorder audit;

    public DepartmentApplicationService(DepartmentRepository departmentRepository,
                                        DataScopeAuthorizationService dataScope,
                                        AuditRecorder audit) {
        this.departmentRepository = departmentRepository;
        this.dataScope = dataScope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listByOrganizationId(Long organizationId) {
        DataScopeResolution scope = dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION");
        if (!covers(scope, organizationId)) return List.of();
        return departmentRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(DepartmentResponse::sortOrder))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION"), dept.organizationId());
        return toResponse(dept);
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest req) {
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:create", "masterdata", "ORGANIZATION"), req.organizationId());
        String actor = currentUsername();
        if (departmentRepository.existsByDeptCode(req.deptCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Department code already exists");
        }
        if (req.parentId() != null) {
            Department parent = departmentRepository.findById(req.parentId()).orElse(null);
            if (parent == null || !parent.organizationId().equals(req.organizationId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Parent department must belong to the same organization");
            }
        }
        int level = 0;
        String path = null;
        if (req.parentId() != null) {
            Department parent = departmentRepository.findById(req.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Parent department not found"));
            level = parent.level() + 1;
            path = (parent.path() != null ? parent.path() + "/" : "/") + parent.id();
        }
        Department dept = new Department(null, req.deptCode(), req.deptName(), req.organizationId(),
                req.parentId(), req.managerName(), req.sortOrder() != null ? req.sortOrder() : 0,
                path, level, "ACTIVE", req.description(),
                actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
        Department saved = departmentRepository.save(dept);
        audit.record("DEPARTMENT_CREATED", "DEPARTMENT", String.valueOf(saved.id()),
                "SUCCESS", null, summary(saved), null);
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest req) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        DataScopeResolution scope = dataScope.requireWrite(
                "masterdata:organization:update", "masterdata", "ORGANIZATION");
        requireCovers(scope, existing.organizationId());
        requireCovers(scope, req.organizationId() != null ? req.organizationId() : existing.organizationId());
        if (req.parentId() != null && req.parentId().equals(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Department cannot be its own parent");
        }
        Department updated = new Department(
                existing.id(), existing.deptCode(),
                req.deptName() != null ? req.deptName() : existing.deptName(),
                req.organizationId() != null ? req.organizationId() : existing.organizationId(),
                req.parentId() != null ? req.parentId() : existing.parentId(),
                req.managerName() != null ? req.managerName() : existing.managerName(),
                req.sortOrder() != null ? req.sortOrder() : existing.sortOrder(),
                existing.path(), existing.level(), existing.status(),
                req.description() != null ? req.description() : existing.description(),
                existing.createdBy(), existing.createdAt(), currentUsername(), LocalDateTime.now(),
                req.version() != null ? req.version() : existing.version());
        Department saved = departmentRepository.update(updated);
        audit.record("DEPARTMENT_UPDATED", "DEPARTMENT", String.valueOf(id),
                "SUCCESS", summary(existing), summary(saved), null);
        return toResponse(saved);
    }

    @Transactional
    public void enable(Long id) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:enable", "masterdata", "ORGANIZATION"), existing.organizationId());
        if (!departmentRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("DEPARTMENT_ENABLED", "DEPARTMENT", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disable(Long id) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:disable", "masterdata", "ORGANIZATION"), existing.organizationId());
        long activeChildren = departmentRepository.countByParentId(id);
        if (activeChildren > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable department with active child departments");
        }
        Map<String, Long> refs = departmentRepository.countActiveRefs(id);
        long totalRefs = refs.values().stream().mapToLong(Long::longValue).sum();
        if (totalRefs > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable department with active positions or users");
        }
        if (!departmentRepository.updateStatus(id, "DISABLED", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("DEPARTMENT_DISABLED", "DEPARTMENT", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=DISABLED", null);
    }

    private DepartmentResponse toResponse(Department dept) {
        return new DepartmentResponse(dept.id(), dept.deptCode(), dept.deptName(),
                dept.organizationId(), null, dept.parentId(), dept.managerName(),
                dept.sortOrder(), dept.path(), dept.level(), dept.status(),
                dept.description(), dept.version(), dept.createdAt(), dept.updatedAt());
    }

    private boolean covers(DataScopeResolution scope, Long organizationId) {
        return scope.isAllScope() || scope.coversOrganization(organizationId);
    }

    private void requireCovers(DataScopeResolution scope, Long organizationId) {
        if (!covers(scope, organizationId)) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String summary(Department d){return "code="+d.deptCode()+",name="+d.deptName()+",organizationId="+d.organizationId()+",status="+d.status();}
}
