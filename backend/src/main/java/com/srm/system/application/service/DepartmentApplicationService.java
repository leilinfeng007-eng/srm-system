package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.system.api.request.CreateDepartmentRequest;
import com.srm.system.api.request.UpdateDepartmentRequest;
import com.srm.system.api.response.DepartmentResponse;
import com.srm.system.domain.model.Department;
import com.srm.system.domain.model.User;
import com.srm.system.domain.repository.DepartmentRepository;
import com.srm.system.domain.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final DataScopeAuthorizationService dataScope;
    private final AuditRecorder audit;

    public DepartmentApplicationService(DepartmentRepository departmentRepository,
                                        UserRepository userRepository,
                                        OrganizationRepository organizationRepository,
                                        DataScopeAuthorizationService dataScope,
                                        AuditRecorder audit) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.dataScope = dataScope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> tree(Long organizationId) {
        DataScopeResolution scope = dataScope.requireRead(
                "system:department:view", "system", "ORGANIZATION");
        if (!covers(scope, organizationId)) return List.of();
        List<Department> all = departmentRepository.findByOrganizationId(organizationId);
        Map<Long, List<Department>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(d -> d.parentId() != null ? d.parentId() : 0L));
        List<Department> roots = childrenMap.getOrDefault(0L, List.of());
        return roots.stream()
                .map(r -> buildTree(r, childrenMap))
                .sorted(Comparator.comparing(DepartmentResponse::sortOrder))
                .collect(Collectors.toList());
    }

    private DepartmentResponse buildTree(Department dept, Map<Long, List<Department>> childrenMap) {
        List<Department> children = childrenMap.getOrDefault(dept.id(), List.of());
        List<DepartmentResponse> childResponses = children.stream()
                .map(c -> buildTree(c, childrenMap))
                .sorted(Comparator.comparing(DepartmentResponse::sortOrder))
                .collect(Collectors.toList());
        return toResponse(dept, childResponses);
    }

    @Transactional(readOnly = true)
    public PageResult<DepartmentResponse> list(String deptCode, String deptName, Long organizationId,
                                                String status, int page, int pageSize) {
        DataScopeResolution scope = dataScope.requireRead(
                "system:department:view", "system", "ORGANIZATION");
        List<Department> all = departmentRepository.findAll();
        List<Department> filtered = all.stream()
                .filter(d -> scope.isAllScope() || scope.coversOrganization(d.organizationId()))
                .filter(d -> deptCode == null || d.deptCode().contains(deptCode))
                .filter(d -> deptName == null || d.deptName().contains(deptName))
                .filter(d -> organizationId == null || d.organizationId().equals(organizationId))
                .filter(d -> status == null || d.status().equals(status))
                .sorted(Comparator.comparing(Department::sortOrder))
                .toList();
        int total = filtered.size();
        int offset = (page - 1) * pageSize;
        List<DepartmentResponse> items = filtered.stream()
                .skip(offset).limit(pageSize)
                .map(d -> toResponse(d, null))
                .collect(Collectors.toList());
        return PageResult.of(items, page, pageSize, total);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireRead(
                "system:department:view", "system", "ORGANIZATION"), dept.organizationId());
        return toResponse(dept, null);
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest req) {
        requireCovers(dataScope.requireWrite(
                "system:department:create", "system", "ORGANIZATION"), req.organizationId());
        String actor = currentUsername();
        if (departmentRepository.existsByDeptCode(req.deptCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门编码已存在");
        }
        if (req.managerId() != null) {
            validateManager(req.managerId(), req.organizationId());
        }
        if (req.parentId() != null) {
            Department parent = departmentRepository.findById(req.parentId()).orElse(null);
            if (parent == null || !parent.organizationId().equals(req.organizationId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上级部门必须属于同一组织");
            }
        }
        int level = 0;
        String path = null;
        if (req.parentId() != null) {
            Department parent = departmentRepository.findById(req.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "上级部门不存在"));
            level = parent.level() + 1;
            path = (parent.path() != null ? parent.path() + "/" : "/") + parent.id();
        }
        Department dept = new Department(null, req.deptCode(), req.deptName(), req.organizationId(),
                req.parentId(), req.managerName(), req.managerId(),
                req.sortOrder() != null ? req.sortOrder() : 0,
                path, level, "ACTIVE", req.description(),
                actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
        Department saved = departmentRepository.save(dept);
        audit.record("DEPARTMENT_CREATED", "DEPARTMENT", String.valueOf(saved.id()),
                "SUCCESS", null, summary(saved), null);
        return toResponse(saved, null);
    }

    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest req) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        DataScopeResolution scope = dataScope.requireWrite(
                "system:department:update", "system", "ORGANIZATION");
        requireCovers(scope, existing.organizationId());
        if (req.organizationId() != null && !req.organizationId().equals(existing.organizationId())) {
            if (!dataScope.anySingleRoleCoversBothOrgs("system:department:update",
                    "system", "ORGANIZATION", existing.organizationId(), req.organizationId())) {
                audit.record("DEPARTMENT_MOVE_BLOCKED", "DEPARTMENT", String.valueOf(id), "FAILURE",
                        "orgId=" + existing.organizationId() + "→" + req.organizationId(),
                        null, "Cross-role write amplification prevented");
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "跨组织移动需要同一角色同时覆盖原组织和目标组织");
            }
            var targetOrg = organizationRepository.findById(req.organizationId());
            if (targetOrg.isEmpty() || !"ACTIVE".equals(targetOrg.get().status())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "目标组织不存在或已停用");
            }
            audit.record("DEPARTMENT_MOVE", "DEPARTMENT", String.valueOf(id), "SUCCESS",
                    "orgId=" + existing.organizationId(), "orgId=" + req.organizationId(), null);
        }
        if (req.parentId() != null && req.parentId().equals(id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不能将自己设为上级部门");
        }
        if (req.parentId() != null && !req.parentId().equals(existing.parentId())) {
            List<Department> children = departmentRepository.findByParentId(id);
            if (children.stream().anyMatch(c -> c.id().equals(req.parentId()))) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不能将下级部门设为上级部门，会形成循环层级");
            }
        }
        if (req.managerId() != null && !req.managerId().equals(existing.managerId())) {
            Long targetOrgId = req.organizationId() != null ? req.organizationId() : existing.organizationId();
            validateManager(req.managerId(), targetOrgId);
        }
        Department updated = new Department(
                existing.id(), existing.deptCode(),
                req.deptName() != null ? req.deptName() : existing.deptName(),
                req.organizationId() != null ? req.organizationId() : existing.organizationId(),
                req.parentId() != null ? req.parentId() : existing.parentId(),
                req.managerName() != null ? req.managerName() : existing.managerName(),
                req.managerId() != null ? req.managerId() : existing.managerId(),
                req.sortOrder() != null ? req.sortOrder() : existing.sortOrder(),
                existing.path(), existing.level(), existing.status(),
                req.description() != null ? req.description() : existing.description(),
                existing.createdBy(), existing.createdAt(), currentUsername(), LocalDateTime.now(),
                req.version() != null ? req.version() : existing.version());
        Department saved = departmentRepository.update(updated);
        audit.record("DEPARTMENT_UPDATED", "DEPARTMENT", String.valueOf(id),
                "SUCCESS", summary(existing), summary(saved), null);
        return toResponse(saved, null);
    }

    @Transactional
    public void enable(Long id) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireWrite(
                "system:department:enable", "system", "ORGANIZATION"), existing.organizationId());
        if (!departmentRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "并发修改冲突，请刷新后重试");
        }
        audit.record("DEPARTMENT_ENABLED", "DEPARTMENT", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disable(Long id) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Department not found"));
        requireCovers(dataScope.requireWrite(
                "system:department:disable", "system", "ORGANIZATION"), existing.organizationId());
        long activeChildren = departmentRepository.countByParentId(id);
        if (activeChildren > 0) {
            audit.record("DEPARTMENT_DISABLE_BLOCKED", "DEPARTMENT", String.valueOf(id), "FAILURE",
                    activeChildren + " active children", null, "Active child departments exist");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法停用：该部门下存在 " + activeChildren + " 个启用的子部门，请先处理子部门");
        }
        Map<String, Long> refs = departmentRepository.countActiveRefs(id);
        long activePositions = refs.getOrDefault("positions", 0L);
        long activeUsers = refs.getOrDefault("users", 0L);
        if (activePositions > 0) {
            audit.record("DEPARTMENT_DISABLE_BLOCKED", "DEPARTMENT", String.valueOf(id), "FAILURE",
                    activePositions + " active positions", null, "Active positions exist");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法停用：该部门下存在 " + activePositions + " 个启用的岗位");
        }
        if (activeUsers > 0) {
            audit.record("DEPARTMENT_DISABLE_BLOCKED", "DEPARTMENT", String.valueOf(id), "FAILURE",
                    activeUsers + " active users", null, "Active users exist");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法停用：该部门下存在 " + activeUsers + " 个启用的用户");
        }
        if (!departmentRepository.updateStatus(id, "DISABLED", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "并发修改冲突，请刷新后重试");
        }
        audit.record("DEPARTMENT_DISABLED", "DEPARTMENT", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=DISABLED", null);
    }

    private void validateManager(Long managerId, Long organizationId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "指定的负责人不存在"));
        if (!"ACTIVE".equals(manager.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "指定的负责人已停用，请选择启用的用户");
        }
        if (manager.mainOrganizationId() != null && !manager.mainOrganizationId().equals(organizationId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "负责人不属于该部门所在组织范围");
        }
    }

    private DepartmentResponse toResponse(Department dept, List<DepartmentResponse> children) {
        Map<String, Long> refs = departmentRepository.countActiveRefs(dept.id());
        return new DepartmentResponse(dept.id(), dept.deptCode(), dept.deptName(),
                dept.organizationId(), null, dept.parentId(), null,
                dept.managerId(), dept.managerName(),
                dept.sortOrder(), dept.path(), dept.level(), dept.status(),
                dept.description(),
                refs.getOrDefault("positions", 0L),
                refs.getOrDefault("users", 0L),
                dept.version(), dept.createdAt(), dept.updatedAt());
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

    private String summary(Department d) {
        return "code=" + d.deptCode() + ",name=" + d.deptName()
                + ",organizationId=" + d.organizationId() + ",status=" + d.status();
    }
}
