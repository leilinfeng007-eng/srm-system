package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.api.request.CreatePositionRequest;
import com.srm.system.api.request.UpdatePositionRequest;
import com.srm.system.api.response.PositionResponse;
import com.srm.system.domain.model.Position;
import com.srm.system.domain.repository.PositionRepository;
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
public class PositionApplicationService {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final DataScopeAuthorizationService dataScope;
    private final AuditRecorder audit;

    public PositionApplicationService(PositionRepository positionRepository,
                                      DepartmentRepository departmentRepository,
                                      DataScopeAuthorizationService dataScope,
                                      AuditRecorder audit) {
        this.positionRepository = positionRepository;
        this.departmentRepository = departmentRepository;
        this.dataScope = dataScope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> listByDepartmentId(Long departmentId) {
        Long organizationId = organizationId(departmentId);
        DataScopeResolution scope = dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION");
        if (!covers(scope, organizationId)) return List.of();
        return positionRepository.findByDepartmentId(departmentId).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PositionResponse::sortOrder))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PositionResponse getById(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        requireCovers(dataScope.requireRead(
                "masterdata:organization:view", "masterdata", "ORGANIZATION"),
                organizationId(position.departmentId()));
        return toResponse(position);
    }

    @Transactional
    public PositionResponse create(CreatePositionRequest req) {
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:create", "masterdata", "ORGANIZATION"),
                organizationId(req.departmentId()));
        String actor = currentUsername();
        if (positionRepository.existsByPositionCode(req.positionCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Position code already exists");
        }
        Position position = new Position(null, req.positionCode(), req.positionName(),
                req.departmentId(), req.responsibility(),
                req.sortOrder() != null ? req.sortOrder() : 0,
                "ACTIVE", actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
        Position saved = positionRepository.save(position);
        audit.record("POSITION_CREATED", "POSITION", String.valueOf(saved.id()),
                "SUCCESS", null, summary(saved), null);
        return toResponse(saved);
    }

    @Transactional
    public PositionResponse update(Long id, UpdatePositionRequest req) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        DataScopeResolution scope = dataScope.requireWrite(
                "masterdata:organization:update", "masterdata", "ORGANIZATION");
        requireCovers(scope, organizationId(existing.departmentId()));
        requireCovers(scope, organizationId(req.departmentId() != null
                ? req.departmentId() : existing.departmentId()));
        Position updated = new Position(
                existing.id(), existing.positionCode(),
                req.positionName() != null ? req.positionName() : existing.positionName(),
                req.departmentId() != null ? req.departmentId() : existing.departmentId(),
                req.responsibility() != null ? req.responsibility() : existing.responsibility(),
                req.sortOrder() != null ? req.sortOrder() : existing.sortOrder(),
                existing.status(), existing.createdBy(), existing.createdAt(),
                currentUsername(), LocalDateTime.now(),
                req.version() != null ? req.version() : existing.version());
        Position saved = positionRepository.update(updated);
        audit.record("POSITION_UPDATED", "POSITION", String.valueOf(id),
                "SUCCESS", summary(existing), summary(saved), null);
        return toResponse(saved);
    }

    @Transactional
    public void enable(Long id) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:enable", "masterdata", "ORGANIZATION"),
                organizationId(existing.departmentId()));
        if (!positionRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("POSITION_ENABLED", "POSITION", String.valueOf(id), "SUCCESS",
                "status="+existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disable(Long id) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        requireCovers(dataScope.requireWrite(
                "masterdata:organization:disable", "masterdata", "ORGANIZATION"),
                organizationId(existing.departmentId()));
        Map<String, Long> refs = positionRepository.countActiveRefs(id);
        if (refs.getOrDefault("users", 0L) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable position referenced by active users");
        }
        if (!positionRepository.updateStatus(id, "DISABLED", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        audit.record("POSITION_DISABLED", "POSITION", String.valueOf(id), "SUCCESS",
                "status="+existing.status(), "status=DISABLED", null);
    }

    private PositionResponse toResponse(Position position) {
        return new PositionResponse(position.id(), position.positionCode(), position.positionName(),
                position.departmentId(), null, position.responsibility(), position.sortOrder(),
                position.status(), position.version(), position.createdAt(), position.updatedAt());
    }

    private Long organizationId(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .map(com.srm.system.domain.model.Department::organizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Department not found"));
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

    private String summary(Position p){return "code="+p.positionCode()+",name="+p.positionName()+",departmentId="+p.departmentId()+",status="+p.status();}
}
