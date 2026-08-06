package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.platform.navigation.TransactionAwareCacheInvalidator;
import com.srm.system.api.request.CreateRoleRequest;
import com.srm.system.api.request.RoleMenuPermissionRequest;
import com.srm.system.api.request.UpdateRoleRequest;
import com.srm.system.api.response.RoleDetailResponse;
import com.srm.system.api.response.RoleHistoryResponse;
import com.srm.system.api.response.RoleResponse;
import com.srm.system.api.response.RoleDataPolicyResponse;
import com.srm.system.api.response.RoleUserResponse;
import com.srm.system.domain.model.Role;
import com.srm.system.domain.model.User;
import com.srm.system.domain.model.UserRoleHistory;
import com.srm.system.domain.repository.RoleRepository;
import com.srm.system.domain.repository.UserRoleHistoryRepository;
import com.srm.system.domain.repository.DataPolicyRequest;
import com.srm.system.domain.repository.UserRepository;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.service.AuditRecorder;
import com.srm.system.domain.service.RoleTransferGuard;
import com.srm.security.auth.SrmPrincipal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final TransactionAwareCacheInvalidator navigationCacheInvalidator;
    private final AuditRecorder auditService;
    private final UserRepository userRepository;
    private final DataScopeAuthorizationService dataScope;
    private final RoleTransferGuard roleTransferGuard;

    public RoleApplicationService(RoleRepository roleRepository,
                                    UserRoleHistoryRepository userRoleHistoryRepository,
                                    TransactionAwareCacheInvalidator navigationCacheInvalidator,
                                    AuditRecorder auditService,
                                    UserRepository userRepository,
                                    DataScopeAuthorizationService dataScope,
                                    RoleTransferGuard roleTransferGuard) {
        this.roleRepository = roleRepository;
        this.userRoleHistoryRepository = userRoleHistoryRepository;
        this.navigationCacheInvalidator = navigationCacheInvalidator;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.dataScope = dataScope;
        this.roleTransferGuard = roleTransferGuard;
    }

    @Transactional(readOnly = true)
    public PageResult<RoleResponse> listRoles(int page, int pageSize) {
        requireAll("system:role:view", false);
        long total = roleRepository.countAll();
        int offset = (page - 1) * pageSize;
        List<Role> roles = roleRepository.findPage(offset, pageSize);
        List<RoleResponse> items = roles.stream().map(r -> {
            long userCount = roleRepository.countActiveUsers(r.id());
            return new RoleResponse(r.id(), r.roleCode(), r.roleName(),
                    r.description(), r.status(), r.builtIn(),
                    r.roleCategory() != null ? r.roleCategory() : "BUSINESS",
                    userCount,
                    r.version(), r.createdAt(), r.updatedAt());
        }).collect(Collectors.toList());
        return PageResult.of(items, page, pageSize, total);
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getRole(Long id) {
        requireAll("system:role:view", false);
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        List<Long> userIds = roleRepository.findUserIdsByRole(id);
        List<Long> menuIds = roleRepository.findMenuIdsByRole(id);
        List<Long> permissionIds = roleRepository.findPermissionIdsByRole(id);
        List<RoleDataPolicyResponse> dataPolicies = roleRepository.findDataPolicies(id).stream()
                .map(p -> new RoleDataPolicyResponse(p.domainCode(), p.dimensionCode(),
                        p.scopeType(), p.includeChildren(), p.operationMode(), p.scopeOrgIds()))
                .toList();
        List<RoleHistoryResponse> history = findRoleHistory(id);
        return new RoleDetailResponse(role.id(), role.roleCode(), role.roleName(),
                role.description(), role.status(), role.builtIn(),
                role.roleCategory() != null ? role.roleCategory() : "BUSINESS",
                role.version(), role.createdAt(), role.updatedAt(),
                userIds, menuIds, permissionIds, dataPolicies, history);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest req) {
        requireAll("system:role:create", true);
        String actor = currentUsername();
        if (roleRepository.existsByRoleCode(req.roleCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Role code already exists");
        }
        Role role = new Role(null, req.roleCode(), req.roleName(), req.description(),
                "ACTIVE", false,
                req.roleCategory() != null ? req.roleCategory() : "BUSINESS",
                actor, LocalDateTime.now(), actor, LocalDateTime.now(), 0L);
        Role saved = roleRepository.save(role);
        auditService.record("ROLE_CREATED", "ROLE", String.valueOf(saved.id()), "SUCCESS",
                null, "code=" + saved.roleCode() + ",name=" + saved.roleName(), null);
        return new RoleResponse(saved.id(), saved.roleCode(), saved.roleName(),
                saved.description(), saved.status(), saved.builtIn(),
                saved.roleCategory() != null ? saved.roleCategory() : "BUSINESS",
                0L, saved.version(), saved.createdAt(), saved.updatedAt());
    }

    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest req) {
        requireAll("system:role:update", true);
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        if (Boolean.TRUE.equals(existing.builtIn())) {
            if (req.roleName() != null && !req.roleName().equals(existing.roleName())) {
                throw new BusinessException(ErrorCode.CONFLICT, "Cannot change role name of built-in role");
            }
        }
        Role updated = new Role(existing.id(), existing.roleCode(),
                req.roleName() != null ? req.roleName() : existing.roleName(),
                req.description() != null ? req.description() : existing.description(),
                existing.status(), existing.builtIn(),
                req.roleCategory() != null ? req.roleCategory() : existing.roleCategory(),
                existing.createdBy(), existing.createdAt(), currentUsername(), LocalDateTime.now(),
                req.version() != null ? req.version() : existing.version());
        Role saved = roleRepository.update(updated);
        auditService.record("ROLE_UPDATED", "ROLE", String.valueOf(id), "SUCCESS",
                "name=" + existing.roleName(), "name=" + saved.roleName(), null);
        long userCount = roleRepository.countActiveUsers(saved.id());
        return new RoleResponse(saved.id(), saved.roleCode(), saved.roleName(),
                saved.description(), saved.status(), saved.builtIn(),
                saved.roleCategory() != null ? saved.roleCategory() : "BUSINESS",
                userCount, saved.version(), saved.createdAt(), saved.updatedAt());
    }

    @Transactional
    public void enableRole(Long id) {
        requireAll("system:role:enable", true);
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        if (!roleRepository.updateStatus(id, "ACTIVE", existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "Concurrent modification detected");
        }
        auditService.record("ROLE_ENABLED", "ROLE", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disableRole(Long id) {
        requireAll("system:role:disable", true);
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        if (Boolean.TRUE.equals(existing.builtIn())) {
            auditService.record("ROLE_DISABLE_BLOCKED", "ROLE", String.valueOf(id), "FAILURE",
                    "builtIn=" + existing.builtIn(), null, "Cannot disable built-in role");
            throw new BusinessException(ErrorCode.CONFLICT, "Cannot disable built-in role");
        }
        long affectedUsers = roleRepository.countUsersLosingAllRoles(id);
        if (affectedUsers > 0) {
            auditService.record("ROLE_DISABLE_BLOCKED", "ROLE", String.valueOf(id), "FAILURE",
                    String.valueOf(affectedUsers) + " users would lose all roles", null,
                    "Users would be left without any active role");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法停用：停用后将导致 " + affectedUsers + " 个用户失去所有有效角色，请先为这些用户重新分配角色");
        }
        if (!roleRepository.updateStatus(id, "DISABLED", existing.version())) {
            auditService.record("ROLE_DISABLE_BLOCKED", "ROLE", String.valueOf(id), "FAILURE",
                    "version=" + existing.version(), null, "Concurrent modification");
            throw new BusinessException(ErrorCode.CONFLICT, "并发修改冲突，请刷新后重试");
        }
        auditService.record("ROLE_DISABLED", "ROLE", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=DISABLED", null);
        navigationCacheInvalidator.evictAll();
    }

    @Transactional
    public void assignUsersToRole(Long roleId, List<Long> userIds) {
        requireAll("system:role:assign-user", true);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        roleTransferGuard.requireRoleEnabled(role);
        requireRoleAdministrationAllowed(role);
        String actor = currentUsername();
        boolean actorIsSuperAdmin = currentUserIsSuperAdmin();
        Set<String> actorPermissions = currentAuthorities();
        List<Long> distinctUserIds = userIds == null ? List.of() : userIds.stream().distinct().toList();
        for (Long userId : distinctUserIds) {
            userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
            roleTransferGuard.requireNotSelfAssignment(userId, currentUserId(), actorIsSuperAdmin);
            roleTransferGuard.requireSuperAdminAdministration(role, actorIsSuperAdmin);
            roleTransferGuard.requireTransferable(role, actorPermissions);
            roleRepository.assignUserToRole(userId, roleId, actor);
            recordUserRoleHistory(userId, roleId, "ASSIGN", null, "ACTIVE", actor);
            auditService.record("ROLE_USER_ASSIGNED", "ROLE", String.valueOf(roleId),
                    "SUCCESS", null, "userId=" + userId, null);
        }
        navigationCacheInvalidator.evictAll();
    }

    @Transactional
    public void removeUserFromRole(Long roleId, Long userId) {
        requireAll("system:role:assign-user", true);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        requireRoleAdministrationAllowed(role);
        if ("SUPER_ADMIN".equals(role.roleCode())) {
            roleRepository.lockActiveAssignments(roleId);
            long remainingAdmins = roleRepository.countActiveUsersExcluding(roleId, userId);
            if (remainingAdmins == 0) {
                auditService.record("ROLE_USER_REMOVE_BLOCKED", "ROLE", String.valueOf(roleId), "FAILURE",
                        "last SUPER_ADMIN", null, "Cannot remove the last SUPER_ADMIN user");
                throw new BusinessException(ErrorCode.CONFLICT, "Cannot remove the last SUPER_ADMIN user");
            }
        }
        List<Long> otherActiveRoleIds = roleRepository.findRoleIdsByUserId(userId).stream()
                .filter(rid -> !rid.equals(roleId)).toList();
        if (otherActiveRoleIds.isEmpty()) {
            auditService.record("ROLE_USER_REMOVE_BLOCKED", "ROLE", String.valueOf(roleId), "FAILURE",
                    "last role for user " + userId, null, "User would lose all roles");
            throw new BusinessException(ErrorCode.CONFLICT,
                    "无法移除：该角色是用户当前唯一有效角色，移除后将导致用户失去所有权限，请先为用户分配其他角色");
        }
        String actor = currentUsername();
        roleRepository.removeUserFromRole(userId, roleId);
        recordUserRoleHistory(userId, roleId, "REMOVE", "ACTIVE", "INACTIVE", actor);
        auditService.record("ROLE_USER_REMOVED", "ROLE", String.valueOf(roleId),
                "SUCCESS", "userId=" + userId, null, null);
        navigationCacheInvalidator.evictUser(userId);
    }

    @Transactional
    public void assignMenusPermissions(Long roleId, RoleMenuPermissionRequest req) {
        requireAll("system:role:assign-permission", true);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        requireRoleAdministrationAllowed(role);
        String actor = currentUsername();
        if (req.menuIds() != null) {
            roleRepository.clearRoleMenus(roleId, actor);
            if (!CollectionUtils.isEmpty(req.menuIds())) {
                roleRepository.addRoleMenus(roleId, req.menuIds(), actor);
            }
        }
        if (req.permissionIds() != null) {
            roleRepository.clearRolePermissions(roleId, actor);
            if (!CollectionUtils.isEmpty(req.permissionIds())) {
                roleRepository.addRolePermissions(roleId, req.permissionIds(), actor);
            }
        }
        recordRoleHistory(roleId, "MENU_PERM_UPDATED", "Menu/perm changed", actor);
        auditService.record("ROLE_AUTHORIZATION_UPDATED", "ROLE", String.valueOf(roleId),
                "SUCCESS", null, "menuIds=" + safeIds(req.menuIds())
                        + ",permissionIds=" + safeIds(req.permissionIds()), null);
        navigationCacheInvalidator.evictAll();
    }

    @Transactional
    public void assignDataPolicies(Long roleId, List<com.srm.system.api.request.RoleDataPolicyRequest> policies) {
        requireAll("system:role:assign-data-scope", true);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        requireRoleAdministrationAllowed(role);
        String actor = currentUsername();

        List<DataPolicyRequest> beforePolicies = roleRepository.findDataPolicies(roleId);

        List<DataPolicyRequest> domainPolicies = policies != null
                ? policies.stream().map(p -> new DataPolicyRequest(
                        p.domainCode(), p.dimensionCode(), p.scopeType(),
                        p.includeChildren(), p.operationMode(), p.scopeOrgIds())).collect(Collectors.toList())
                : Collections.emptyList();
        validatePolicies(domainPolicies);
        validateAdminScope(domainPolicies);
        roleRepository.replaceDataPolicies(roleId, domainPolicies, actor);

        List<DataPolicyRequest> afterPolicies = roleRepository.findDataPolicies(roleId);
        auditService.record("ROLE_DATA_POLICY_UPDATED", "ROLE", String.valueOf(roleId),
                "SUCCESS", policiesSummary(beforePolicies), policiesSummary(afterPolicies), actor);

        recordRoleHistory(roleId, "DATA_POLICY_UPDATED",
                "Data policies: " + domainPolicies.size(), actor);
        navigationCacheInvalidator.evictAll();
    }

    @Transactional(readOnly = true)
    public List<RoleUserResponse> getRoleUsers(Long roleId) {
        requireAll("system:role:view", false);
        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        return roleRepository.findUserIdsByRole(roleId).stream()
                .map(userRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(u -> new RoleUserResponse(u.id(), u.username(), u.displayName(), u.status()))
                .toList();
    }

    private void validatePolicies(List<DataPolicyRequest> policies) {
        var dimensions = java.util.Set.of("ORGANIZATION", "PURCHASING_ORGANIZATION",
                "PLANT", "CATEGORY", "OWNER", "SUPPLIER");
        for (DataPolicyRequest policy : policies) {
            if (!java.util.Set.of("system", "masterdata").contains(policy.domainCode())
                    || !dimensions.contains(policy.dimensionCode())
                    || !java.util.Set.of("ALL", "ORG", "SELF").contains(policy.scopeType())
                    || !java.util.Set.of("READ_WRITE", "READONLY").contains(policy.operationMode())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid data policy");
            }
            if ("SELF".equals(policy.scopeType()) && !"OWNER".equals(policy.dimensionCode())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "SELF scope is only valid for the OWNER dimension");
            }
        }
    }

    private void validateAdminScope(List<DataPolicyRequest> policies) {
        for (DataPolicyRequest policy : policies) {
            String scopeOrgIds = policy.scopeOrgIds();
            if (scopeOrgIds == null || scopeOrgIds.isBlank()) continue;
            if (!"ORG".equals(policy.scopeType())) continue;
            DataScopeResolution adminScope = dataScope.resolveForWrite(
                    "system:role:assign-data-scope", "system", "ORGANIZATION");
            if (!adminScope.isAllScope()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<Long> ids = om.readValue(scopeOrgIds,
                            new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                    for (Long orgId : ids) {
                        if (!adminScope.coversOrganization(orgId)) {
                            auditService.record("DATA_POLICY_SCOPE_BLOCKED", "ROLE", "scope_org_ids",
                                    "FAILURE", null, scopeOrgIds,
                                    "Admin scope does not cover org " + orgId);
                            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                                    "无权配置超出自身管理范围的数据策略");
                        }
                    }
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid scope_org_ids JSON");
                }
            }
        }
    }

    private void requireRoleAdministrationAllowed(Role targetRole) {
        if ("SUPER_ADMIN".equals(targetRole.roleCode()) && !currentUserIsSuperAdmin()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Only an existing SUPER_ADMIN can change SUPER_ADMIN authorization");
        }
    }

    private boolean currentUserIsSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private String policiesSummary(List<DataPolicyRequest> policies) {
        StringBuilder sb = new StringBuilder();
        for (DataPolicyRequest p : policies) {
            sb.append(p.domainCode()).append(':').append(p.dimensionCode())
              .append('=').append(p.scopeType()).append(';');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<RoleHistoryResponse> getRoleHistory(Long roleId) {
        requireAll("system:role:view", false);
        return findRoleHistory(roleId);
    }

    private void requireAll(String permission, boolean write) {
        var scope = write
                ? dataScope.requireWrite(permission, "system", "ORGANIZATION")
                : dataScope.requireRead(permission, "system", "ORGANIZATION");
        if (!scope.isAllScope()) {
            auditService.record("DATA_SCOPE_ACCESS_DENIED", "SYSTEM", permission, "FAILURE",
                    "scope=" + scope.scopeType(), null, "Not ALL scope for " + permission);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private List<RoleHistoryResponse> findRoleHistory(Long roleId) {
        List<UserRoleHistory> histories = userRoleHistoryRepository.findByRoleId(roleId);
        if (histories == null) {
            return Collections.emptyList();
        }
        return histories.stream()
                .map(h -> new RoleHistoryResponse(h.id(), h.action(), h.previousStatus(),
                        h.changedBy(), h.changedAt()))
                .collect(Collectors.toList());
    }

    private void recordUserRoleHistory(Long userId, Long roleId, String action,
                                        String previousStatus, String newStatus, String actor) {
        UserRoleHistory history = new UserRoleHistory(null, userId, roleId, action,
                previousStatus, newStatus, actor, LocalDateTime.now());
        userRoleHistoryRepository.save(history);
    }

    private void recordRoleHistory(Long roleId, String action, String description, String actor) {
        UserRoleHistory history = new UserRoleHistory(null, currentUserId(), roleId, action,
                null, description, actor, LocalDateTime.now());
        userRoleHistoryRepository.save(history);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Set<String> currentAuthorities() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Collections.emptySet();
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private String safeIds(List<Long> values) {
        return values == null ? "unchanged" : values.stream().limit(100)
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }
}
