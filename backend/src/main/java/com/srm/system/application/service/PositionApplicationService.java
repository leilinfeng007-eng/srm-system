package com.srm.system.application.service;

import com.srm.common.api.PageResult;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionApplicationService {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "MANAGEMENT", "TECHNICAL", "PROCUREMENT", "QUALITY",
            "LOGISTICS", "FINANCE", "ADMIN", "OTHER");

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
    public PageResult<PositionResponse> list(String positionCode, String positionName,
                                              Long departmentId, String category,
                                              String status, int page, int pageSize) {
        DataScopeResolution scope = dataScope.requireRead(
                "system:position:view", "system", "ORGANIZATION");
        List<Position> all = positionRepository.findAll();
        List<Position> filtered = all.stream()
                .filter(p -> {
                    Long orgId = organizationId(p.departmentId());
                    return scope.isAllScope() || scope.coversOrganization(orgId);
                })
                .filter(p -> positionCode == null || p.positionCode().contains(positionCode))
                .filter(p -> positionName == null || p.positionName().contains(positionName))
                .filter(p -> departmentId == null || p.departmentId().equals(departmentId))
                .filter(p -> category == null || (p.category() != null && p.category().equals(category)))
                .filter(p -> status == null || p.status().equals(status))
                .sorted(Comparator.comparing(Position::sortOrder))
                .toList();
        int total = filtered.size();
        int offset = (page - 1) * pageSize;
        List<PositionResponse> items = filtered.stream()
                .skip(offset).limit(pageSize)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResult.of(items, page, pageSize, total);
    }

    @Transactional(readOnly = true)
    public PositionResponse getById(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        requireCovers(dataScope.requireRead(
                "system:position:view", "system", "ORGANIZATION"),
                organizationId(position.departmentId()));
        return toResponse(position);
    }

    @Transactional
    public PositionResponse create(CreatePositionRequest req) {
        requireCovers(dataScope.requireWrite(
                "system:position:create", "system", "ORGANIZATION"),
                organizationId(req.departmentId()));
        String actor = currentUsername();
        if (positionRepository.existsByPositionCode(req.positionCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "岗位编码已存在");
        }
        if (req.category() != null && !VALID_CATEGORIES.contains(req.category())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "无效的岗位类别编码: " + req.category());
        }
        var dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "所属部门不存在"));
        if (!"ACTIVE".equals(dept.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "所属部门已停用");
        }
        Position position = new Position(null, req.positionCode(), req.positionName(),
                req.departmentId(), req.category(), req.responsibility(),
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
                "system:position:update", "system", "ORGANIZATION");
        requireCovers(scope, organizationId(existing.departmentId()));
        if (req.category() != null && !VALID_CATEGORIES.contains(req.category())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "无效的岗位类别编码: " + req.category());
        }
        if (req.departmentId() != null && !req.departmentId().equals(existing.departmentId())) {
            Long targetOrgId = organizationId(req.departmentId());
            if (!dataScope.anySingleRoleCoversBothOrgs("system:position:update",
                    "system", "ORGANIZATION", organizationId(existing.departmentId()), targetOrgId)) {
                audit.record("POSITION_MOVE_BLOCKED", "POSITION", String.valueOf(id), "FAILURE",
                        "deptId=" + existing.departmentId() + "→" + req.departmentId(),
                        null, "Cross-role write amplification prevented");
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "跨部门移动需要同一角色同时覆盖原部门和目标部门所在组织");
            }
            var newDept = departmentRepository.findById(req.departmentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目标部门不存在"));
            if (!"ACTIVE".equals(newDept.status())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "目标部门已停用");
            }
            long activeUsers = positionRepository.countActiveRefs(id).getOrDefault("users", 0L);
            if (activeUsers > 0) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "当前岗位下存在 " + activeUsers + " 个启用用户，请先调整人员后再变更所属部门");
            }
        }
        Position updated = new Position(
                existing.id(), existing.positionCode(),
                req.positionName() != null ? req.positionName() : existing.positionName(),
                req.departmentId() != null ? req.departmentId() : existing.departmentId(),
                req.category() != null ? req.category() : existing.category(),
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
                "system:position:enable", "system", "ORGANIZATION"),
                organizationId(existing.departmentId()));
        if (!positionRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "并发修改冲突，请刷新后重试");
        }
        audit.record("POSITION_ENABLED", "POSITION", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disable(Long id) {
        Position existing = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Position not found"));
        requireCovers(dataScope.requireWrite(
                "system:position:disable", "system", "ORGANIZATION"),
                organizationId(existing.departmentId()));
        Map<String, Long> refs = positionRepository.countActiveRefs(id);
        long activeUsers = refs.getOrDefault("users", 0L);
        if (activeUsers > 0) {
            audit.record("POSITION_DISABLE_BLOCKED", "POSITION", String.valueOf(id), "FAILURE",
                    activeUsers + " active users", null, "Active users reference this position");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法停用：当前岗位下有 " + activeUsers + " 个启用的用户，请先调整人员");
        }
        if (!positionRepository.updateStatus(id, "DISABLED", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "并发修改冲突，请刷新后重试");
        }
        audit.record("POSITION_DISABLED", "POSITION", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=DISABLED", null);
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> listByDepartmentId(Long departmentId) {
        Long organizationId = organizationId(departmentId);
        DataScopeResolution scope = dataScope.requireRead(
                "system:position:view", "system", "ORGANIZATION");
        if (!covers(scope, organizationId)) return List.of();
        return positionRepository.findByDepartmentId(departmentId).stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(PositionResponse::sortOrder))
                .collect(Collectors.toList());
    }

    private PositionResponse toResponse(Position position) {
        Map<String, Long> refs = positionRepository.countActiveRefs(position.id());
        return new PositionResponse(position.id(), position.positionCode(), position.positionName(),
                position.departmentId(), null, position.category(), position.responsibility(),
                position.sortOrder(), position.status(),
                refs.getOrDefault("users", 0L),
                position.version(), position.createdAt(), position.updatedAt());
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
        if (!covers(scope, organizationId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String summary(Position p) {
        return "code=" + p.positionCode() + ",name=" + p.positionName()
                + ",departmentId=" + p.departmentId() + ",status=" + p.status();
    }
}
