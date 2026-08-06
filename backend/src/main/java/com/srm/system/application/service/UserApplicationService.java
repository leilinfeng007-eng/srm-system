package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.masterdata.domain.model.Organization;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import com.srm.security.auth.SrmPrincipal;
import com.srm.system.api.request.CreateUserRequest;
import com.srm.system.api.request.UpdateUserRequest;
import com.srm.system.api.response.AssignmentHistoryResponse;
import com.srm.system.api.response.AuthorizationRecordResponse;
import com.srm.system.api.response.ResetPasswordResponse;
import com.srm.system.api.response.UserDetailResponse;
import com.srm.system.api.response.UserEffectivePermissionsView;
import com.srm.system.api.response.UserResponse;
import com.srm.system.api.response.UserRoleSelectionResponse;
import com.srm.system.domain.model.EffectivePermissionGrant;
import com.srm.system.domain.model.Role;
import com.srm.system.domain.model.User;
import com.srm.system.domain.model.UserAssignmentHistory;
import com.srm.system.domain.model.UserOperationLogEntry;
import com.srm.system.domain.model.UserRoleHistory;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.repository.AuthorizationCatalogRepository;
import com.srm.system.domain.repository.DataPolicyRequest;
import com.srm.system.domain.repository.DepartmentRepository;
import com.srm.system.domain.repository.PositionRepository;
import com.srm.system.domain.repository.RoleRepository;
import com.srm.system.domain.repository.UserAssignmentHistoryRepository;
import com.srm.system.domain.repository.UserOperationLogRepository;
import com.srm.system.domain.repository.UserRepository;
import com.srm.system.domain.repository.UserRoleHistoryRepository;
import com.srm.system.domain.service.AuditRecorder;
import com.srm.system.domain.service.RoleTransferGuard;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class UserApplicationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> USER_OPERATION_LOG_ACTIONS = Set.of(
            "USER_CREATED", "USER_UPDATED", "USER_ENABLED", "USER_DISABLED",
            "USER_PASSWORD_RESET", "USER_PASSWORD_CHANGED");
    private static final Map<String, String> ACTION_LABELS = Map.of(
            "ASSIGN", "增加角色",
            "REMOVE", "移除角色",
            "USER_CREATED", "创建用户",
            "USER_UPDATED", "编辑用户",
            "USER_ENABLED", "启用用户",
            "USER_DISABLED", "停用用户",
            "USER_PASSWORD_RESET", "重置密码");

    private final UserRepository userRepository;
    private final UserAssignmentHistoryRepository assignmentHistoryRepository;
    private final UserRoleHistoryRepository userRoleHistoryRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final RoleRepository roleRepository;
    private final AuthorizationCatalogRepository authorizationCatalogRepository;
    private final UserOperationLogRepository userOperationLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeAuthorizationService dataScope;
    private final RoleTransferGuard roleTransferGuard;
    private final AuditRecorder audit;

    public UserApplicationService(UserRepository userRepository,
                                  UserAssignmentHistoryRepository assignmentHistoryRepository,
                                  UserRoleHistoryRepository userRoleHistoryRepository,
                                  OrganizationRepository organizationRepository,
                                  DepartmentRepository departmentRepository,
                                  PositionRepository positionRepository,
                                  RoleRepository roleRepository,
                                  AuthorizationCatalogRepository authorizationCatalogRepository,
                                  UserOperationLogRepository userOperationLogRepository,
                                  PasswordEncoder passwordEncoder,
                                  DataScopeAuthorizationService dataScope,
                                  RoleTransferGuard roleTransferGuard,
                                  AuditRecorder audit) {
        this.userRepository = userRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.userRoleHistoryRepository = userRoleHistoryRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.roleRepository = roleRepository;
        this.authorizationCatalogRepository = authorizationCatalogRepository;
        this.userOperationLogRepository = userOperationLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataScope = dataScope;
        this.roleTransferGuard = roleTransferGuard;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> listUsers(int page, int pageSize, String username, String displayName, String status) {
        DataScopeResolution scope = dataScope.requireRead(
                "system:user:view", "system", "ORGANIZATION");
        long total = userRepository.count(username, displayName, status,
                scope.allowedOrgIds(), scope.isAllScope());
        int offset = (page - 1) * pageSize;
        List<User> users = userRepository.findPage(username, displayName, status,
                scope.allowedOrgIds(), scope.isAllScope(), offset, pageSize);
        List<UserResponse> items = users.stream().map(this::toUserResponse).collect(Collectors.toList());
        return PageResult.of(items, page, pageSize, total);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireRead("system:user:view", "system", "ORGANIZATION"), user);
        List<String> roles = userRepository.findActiveRoleCodes(id);
        List<AssignmentHistoryResponse> history = assignmentHistoryRepository.findByUserId(id)
                .stream().map(h -> new AssignmentHistoryResponse(
                        h.fieldName(), h.oldValue(), h.newValue(),
                        h.changeReason(), h.changedBy(), h.changedAt()))
                .collect(Collectors.toList());
        return toUserDetailResponse(user, roles, history);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        String actor = currentUsername();
        if (userRepository.countByUsername(req.username()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户账号已存在");
        }
        if (req.employeeCode() != null && !req.employeeCode().isBlank()
                && userRepository.countByEmployeeCode(req.employeeCode()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "工号已存在");
        }
        DataScopeResolution writeScope = dataScope.requireWrite(
                "system:user:create", "system", "ORGANIZATION");
        requireOrganization(writeScope, req.mainOrganizationId());
        validateAssignment(req.mainOrganizationId(), req.mainDepartmentId(), req.mainPositionId());
        List<Long> assignedRoleIds = validateRoleAssignment(req.roleIds());
        Instant now = Instant.now();
        String passwordHash = passwordEncoder.encode(req.password());
        String status = CollectionUtils.isEmpty(assignedRoleIds) ? "DISABLED" : "ACTIVE";
        User user = new User(null, req.username(), passwordHash, req.displayName(),
                req.employeeCode(), req.email(), req.phone(),
                req.mainOrganizationId(), req.mainDepartmentId(), req.mainPositionId(),
                true, status, null, now, actor, actor, now, 0L);
        User saved = saveUserSafely(user);
        assignRolesOnCreate(saved.id(), assignedRoleIds, actor);
        audit.record("USER_CREATED", "USER", String.valueOf(saved.id()), "SUCCESS",
                null, summary(saved), null);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        DataScopeResolution writeScope = dataScope.requireWrite(
                "system:user:update", "system", "ORGANIZATION");
        requireVisible(writeScope, existing);
        if (req.version() != null && !req.version().equals(existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "用户已被其他操作修改，请刷新后重试");
        }
        if (req.employeeCode() != null && !req.employeeCode().isBlank()
                && !req.employeeCode().equals(existing.employeeCode())
                && userRepository.countByEmployeeCodeExcluding(req.employeeCode(), id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "工号已存在");
        }
        String actor = currentUsername();
        Long effectiveOrgId = req.mainOrganizationId() != null
                ? req.mainOrganizationId() : existing.mainOrganizationId();
        Long effectiveDepartmentId = req.mainDepartmentId() != null
                ? req.mainDepartmentId() : existing.mainDepartmentId();
        Long effectivePositionId = req.mainPositionId() != null
                ? req.mainPositionId() : existing.mainPositionId();
        validateAssignment(effectiveOrgId, effectiveDepartmentId, effectivePositionId);
        requireOrganization(writeScope, effectiveOrgId);
        if (req.mainOrganizationId() != null && !req.mainOrganizationId().equals(existing.mainOrganizationId())) {
            recordAssignmentHistory(id, "main_organization_id",
                    String.valueOf(existing.mainOrganizationId()),
                    String.valueOf(req.mainOrganizationId()), actor);
        }
        if (req.mainDepartmentId() != null && !req.mainDepartmentId().equals(existing.mainDepartmentId())) {
            recordAssignmentHistory(id, "main_department_id",
                    String.valueOf(existing.mainDepartmentId()),
                    String.valueOf(req.mainDepartmentId()), actor);
        }
        if (req.mainPositionId() != null && !req.mainPositionId().equals(existing.mainPositionId())) {
            recordAssignmentHistory(id, "main_position_id",
                    String.valueOf(existing.mainPositionId()),
                    String.valueOf(req.mainPositionId()), actor);
        }
        User updated = new User(
                existing.id(), existing.username(), existing.passwordHash(),
                req.displayName() != null ? req.displayName() : existing.displayName(),
                req.employeeCode() != null ? req.employeeCode() : existing.employeeCode(),
                req.email() != null ? req.email() : existing.email(),
                req.phone() != null ? req.phone() : existing.phone(),
                effectiveOrgId, effectiveDepartmentId, effectivePositionId,
                existing.mustChangePassword(), existing.status(),
                existing.lastLoginAt(), existing.createdAt(),
                existing.createdBy(), actor, Instant.now(),
                existing.version());
        User saved = userRepository.update(updated);
        audit.record("USER_UPDATED", "USER", String.valueOf(id), "SUCCESS",
                summary(existing), summary(saved), null);
        return toUserResponse(saved);
    }
    @Transactional
    public void enableUser(Long id, Long version) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireWrite(
                "system:user:enable", "system", "ORGANIZATION"), existing);
        if (version != null && !version.equals(existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已被其他操作修改，请刷新后重试");
        }
        if (userRepository.findActiveRoleCodes(id).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "该用户没有任何有效角色，无法启用，请先分配启用状态的角色");
        }
        userRepository.updateStatus(id, "ACTIVE");
        audit.record("USER_ENABLED", "USER", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disableUser(Long id, Long version) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireWrite(
                "system:user:disable", "system", "ORGANIZATION"), existing);
        if (version != null && !version.equals(existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户已被其他操作修改，请刷新后重试");
        }
        userRepository.updateStatus(id, "DISABLED");
        userRepository.revokeSessions(id);
        audit.record("USER_DISABLED", "USER", String.valueOf(id), "SUCCESS",
                "status=" + existing.status(), "status=DISABLED,sessions=revoked", null);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireWrite(
                "system:user:reset-password", "system", "ORGANIZATION"), existing);
        String tempPassword = generateRandomPassword(12);
        String hash = passwordEncoder.encode(tempPassword);
        userRepository.updatePassword(id, hash, true, currentUsername());
        userRepository.revokeSessions(id);
        audit.record("USER_PASSWORD_RESET", "USER", String.valueOf(id), "SUCCESS",
                null, "mustChangePassword=true,sessions=revoked", null);
        return new ResetPasswordResponse(tempPassword);
    }

    @Transactional(readOnly = true)
    public List<UserRoleSelectionResponse> getAssignableRoles() {
        boolean canAssign = hasAuthority("system:user:assign-role");
        Set<String> actorPermissions = canAssign ? currentAuthorities() : Collections.emptySet();
        return roleRepository.findPage(0, 1000).stream()
                .sorted(Comparator.comparing(Role::roleCode))
                .filter(role -> "ACTIVE".equals(role.status())
                        && roleTransferGuard.canGrant(role, actorPermissions))
                .map(role -> new UserRoleSelectionResponse(
                        role.id(), role.roleCode(), role.roleName(), role.status(),
                        role.builtIn(), false, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserRoleSelectionResponse> getRoleSelection(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireRead("system:user:view", "system", "ORGANIZATION"), user);
        List<Long> assignedRoleIds = roleRepository.findRoleIdsByUserId(userId);
        Set<Long> assigned = Set.copyOf(assignedRoleIds);
        boolean canAssign = hasAuthority("system:user:assign-role");
        Set<String> actorPermissions = canAssign ? currentAuthorities() : Collections.emptySet();
        List<Role> roles = roleRepository.findPage(0, 1000);
        boolean actorIsSuperAdmin = actorPermissions.contains("ROLE_SUPER_ADMIN");
        boolean isSelf = currentUserId() != null && currentUserId().equals(userId);
        return roles.stream()
                .sorted(Comparator.comparing(Role::roleCode))
                .map(role -> {
                    boolean selfAddBlocked = isSelf && !actorIsSuperAdmin && !assigned.contains(role.id());
                    return new UserRoleSelectionResponse(
                            role.id(), role.roleCode(), role.roleName(), role.status(),
                            role.builtIn(), assigned.contains(role.id()),
                            canAssign && "ACTIVE".equals(role.status())
                                    && roleTransferGuard.canGrant(role, actorPermissions)
                                    && !selfAddBlocked);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void replaceRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireWrite(
                "system:user:assign-role", "system", "ORGANIZATION"), user);
        List<Long> requested = roleIds == null ? List.of() : roleIds.stream()
                .distinct().collect(Collectors.toList());
        if (requested.size() != (roleIds == null ? 0 : roleIds.size())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "同一角色不能重复分配");
        }
        Map<Long, Role> roleById = new LinkedHashMap<>();
        for (Long roleId : requested) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在"));
            if (!"ACTIVE".equals(role.status())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "角色【" + role.roleName() + "】已停用，不能分配");
            }
            roleById.put(roleId, role);
        }
        String actor = currentUsername();
        Long actorId = currentUserId();
        boolean actorIsSuperAdmin = hasAuthority("ROLE_SUPER_ADMIN");
        List<Long> current = roleRepository.findRoleIdsByUserId(userId);
        Set<Long> currentSet = Set.copyOf(current);
        List<Long> toAdd = requested.stream().filter(id -> !currentSet.contains(id)).toList();
        List<Long> toRemove = current.stream().filter(id -> !roleById.containsKey(id)).toList();
        if (!toAdd.isEmpty()) {
            roleTransferGuard.requireNotSelfAssignment(userId, actorId, actorIsSuperAdmin);
            Set<String> actorPermissions = currentAuthorities();
            for (Long roleId : toAdd) {
                Role role = roleById.get(roleId);
                roleTransferGuard.requireSuperAdminAdministration(role, actorIsSuperAdmin);
                roleTransferGuard.requireTransferable(role, actorPermissions);
            }
        }
        for (Long roleId : toRemove) {
            roleTransferGuard.requireSuperAdminAdministration(
                    roleRepository.findById(roleId).orElse(null), actorIsSuperAdmin);
        }
        Long removedSuperAdminId = null;
        for (Long roleId : toRemove) {
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null && "SUPER_ADMIN".equals(role.roleCode())) {
                removedSuperAdminId = roleId;
                break;
            }
        }
        if (removedSuperAdminId != null) {
            roleRepository.lockActiveAssignments(removedSuperAdminId);
            if (roleRepository.countActiveUsersExcluding(removedSuperAdminId, userId) == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "不能移除最后一个超级管理员");
            }
        }
        if (toAdd.isEmpty() && !toRemove.isEmpty()
                && (current.size() - toRemove.size()) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "不能移除用户的所有角色，请先分配其他角色");
        }
        for (Long roleId : toAdd) {
            Role role = roleById.get(roleId);
            roleRepository.assignUserToRole(userId, roleId, actor);
            recordUserRoleHistory(userId, roleId, "ASSIGN", null, "ACTIVE", actor);
            audit.record("ROLE_USER_ASSIGNED", "ROLE", String.valueOf(roleId), "SUCCESS",
                    null, "userId=" + userId + ",roleId=" + roleId, null);
        }
        for (Long roleId : toRemove) {
            roleRepository.removeUserFromRole(userId, roleId);
            recordUserRoleHistory(userId, roleId, "REMOVE", "ACTIVE", "INACTIVE", actor);
            audit.record("ROLE_USER_REMOVED", "ROLE", String.valueOf(roleId), "SUCCESS",
                    "userId=" + userId + ",roleId=" + roleId, null, null);
        }
        if (userRepository.findActiveRoleCodes(userId).isEmpty() && "ACTIVE".equals(user.status())) {
            userRepository.updateStatus(userId, "DISABLED");
            userRepository.revokeSessions(userId);
            audit.record("USER_DISABLED", "USER", String.valueOf(userId), "SUCCESS",
                    "status=ACTIVE", "status=DISABLED,reason=no-effective-role", null);
        }
    }

    @Transactional(readOnly = true)
    public List<AuthorizationRecordResponse> getAuthorizationHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireRead(
                "system:user:view-authorization", "system", "ORGANIZATION"), user);
        Map<Long, String> roleNamesById = roleRepository.findPage(0, 1000).stream()
                .collect(Collectors.toMap(Role::id, Role::roleName, (a, b) -> a));
        List<AuthorizationRecordResponse> records = new ArrayList<>();
        for (UserRoleHistory history : userRoleHistoryRepository.findByUserId(userId)) {
            String roleName = roleNamesById.getOrDefault(history.roleId(), "角色#" + history.roleId());
            records.add(new AuthorizationRecordResponse(
                    history.changedAt(), history.action(),
                    ACTION_LABELS.getOrDefault(history.action(), history.action()),
                    history.roleId(), roleName,
                    history.previousStatus() == null ? null : "关系状态=" + history.previousStatus(),
                    "关系状态=" + history.newStatus(),
                    "SUCCESS", history.changedBy()));
        }
        for (UserOperationLogEntry log : userOperationLogRepository.findByTargetUser(
                userId, List.copyOf(USER_OPERATION_LOG_ACTIONS))) {
            records.add(new AuthorizationRecordResponse(
                    LocalDateTime.ofInstant(log.occurredAt(), java.time.ZoneOffset.UTC),
                    log.actionCode(),
                    ACTION_LABELS.getOrDefault(log.actionCode(), log.actionCode()),
                    null, null,
                    log.beforeSummary(),
                    log.afterSummary(),
                    log.resultCode(),
                    log.changedBy()));
        }
        records.sort(Comparator.comparing(AuthorizationRecordResponse::occurredAt).reversed());
        return records;
    }

    @Transactional(readOnly = true)
    public UserEffectivePermissionsView getEffectivePermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireRead(
                "system:user:view-permissions", "system", "ORGANIZATION"), user);
        List<EffectivePermissionGrant> grants = authorizationCatalogRepository
                .findEffectivePermissionGrants(userId);
        Map<String, List<EffectivePermissionGrant>> byPermission = grants.stream()
                .collect(Collectors.groupingBy(EffectivePermissionGrant::permissionCode,
                        LinkedHashMap::new, Collectors.toList()));
        List<UserEffectivePermissionsView.UserEffectivePermissionItem> permissions =
                byPermission.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            String code = entry.getKey();
                            String[] parts = code.split(":", 3);
                            return new UserEffectivePermissionsView.UserEffectivePermissionItem(
                                    code,
                                    parts.length > 0 ? parts[0] : code,
                                    parts.length > 1 ? parts[1] : code,
                                    parts.length > 2 ? parts[2] : "",
                                    entry.getValue().stream()
                                            .map(EffectivePermissionGrant::roleCode)
                                            .distinct().sorted().toList());
                        }).toList();

        Map<Long, Role> rolesById = roleRepository.findPage(0, 1000).stream()
                .collect(Collectors.toMap(Role::id, Function.identity(), (a, b) -> a));
        List<Long> assignedRoleIds = roleRepository.findRoleIdsByUserId(userId);
        List<UserEffectivePermissionsView.RoleDataScopeView> roleScopes = assignedRoleIds.stream()
                .map(rolesById::get)
                .filter(role -> role != null && "ACTIVE".equals(role.status()))
                .map(role -> {
                    List<DataPolicyRequest> policies = roleRepository.findDataPolicies(role.id());
                    List<UserEffectivePermissionsView.DataScopePolicyView> views = policies.stream()
                            .map(policy -> scopePolicyView(policy, user))
                            .toList();
                    return new UserEffectivePermissionsView.RoleDataScopeView(
                            role.id(), role.roleCode(), role.roleName(), views);
                }).toList();

        Set<String> effectivePermissionCodes = permissions.stream()
                .map(UserEffectivePermissionsView.UserEffectivePermissionItem::permissionCode)
                .collect(Collectors.toSet());
        long accessibleMenuCount = authorizationCatalogRepository.findEnabledMenus().stream()
                .filter(menu -> menu.permissionCode() == null
                        || effectivePermissionCodes.contains(menu.permissionCode()))
                .count();
        DataScopeResolution readScope = dataScope.resolveForRead(
                "system:user:view", "system", "ORGANIZATION");
        return new UserEffectivePermissionsView(
                userId, user.username(), user.status(),
                roleScopes.size(),
                (int) accessibleMenuCount,
                permissions.size(),
                scopeLabel(readScope),
                permissions, roleScopes);
    }

    private UserEffectivePermissionsView.DataScopePolicyView scopePolicyView(
            DataPolicyRequest policy, User user) {
        boolean effective = "ACTIVE".equals(user.status());
        String ineffectiveReason = effective ? null : "用户已停用，权限暂不生效";
        return new UserEffectivePermissionsView.DataScopePolicyView(
                policy.domainCode(), policy.dimensionCode(),
                policy.scopeType(), scopeTypeLabel(policy.scopeType()),
                policy.includeChildren(), policy.operationMode(),
                effective, ineffectiveReason);
    }

    private String scopeLabel(DataScopeResolution scope) {
        if (scope == null || !scope.canView()) return "无";
        if (scope.isAllScope()) return "全部组织";
        if (scope.isOrgScope()) {
            return scope.allowedOrgIds() != null && scope.allowedOrgIds().size() > 1
                    ? "本组织及下级" : "本组织";
        }
        if (scope.isSelfScope()) return "仅本人";
        return "无";
    }

    private String scopeTypeLabel(String scopeType) {
        return switch (scopeType == null ? "" : scopeType) {
            case "ALL" -> "全部组织";
            case "ORG" -> "本组织及下级";
            case "SELF" -> "仅本人";
            default -> "拒绝访问";
        };
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String username = currentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(oldPassword, user.passwordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword), false, username);
        audit.record("USER_PASSWORD_CHANGED", "USER", String.valueOf(user.id()), "SUCCESS",
                null, "mustChangePassword=false", null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentHistoryResponse> getAssignmentHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireRead(
                "system:user:view", "system", "ORGANIZATION"), user);
        return assignmentHistoryRepository.findByUserId(userId).stream()
                .map(h -> new AssignmentHistoryResponse(
                        h.fieldName(), h.oldValue(), h.newValue(),
                        h.changeReason(), h.changedBy(), h.changedAt()))
                .collect(Collectors.toList());
    }

    private void assignRolesOnCreate(Long userId, List<Long> roleIds, String actor) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Set<String> actorPermissions = currentAuthorities();
        boolean actorIsSuperAdmin = actorPermissions.contains("ROLE_SUPER_ADMIN");
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId).orElseThrow();
            roleTransferGuard.requireSuperAdminAdministration(role, actorIsSuperAdmin);
            roleTransferGuard.requireTransferable(role, actorPermissions);
            roleRepository.assignUserToRole(userId, roleId, actor);
            recordUserRoleHistory(userId, roleId, "ASSIGN", null, "ACTIVE", actor);
        }
    }

    private List<Long> validateRoleAssignment(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<Long> distinct = roleIds.stream().distinct().collect(Collectors.toList());
        if (distinct.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "同一角色不能重复分配");
        }
        for (Long roleId : distinct) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在"));
            roleTransferGuard.requireRoleEnabled(role);
        }
        return distinct;
    }

    private boolean hasAuthority(String authority) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Set<String> currentAuthorities() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Collections.emptySet();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private void recordUserRoleHistory(Long userId, Long roleId, String action,
                                       String previousStatus, String newStatus, String actor) {
        UserRoleHistory history = new UserRoleHistory(null, userId, roleId, action,
                previousStatus, newStatus, actor, LocalDateTime.now());
        userRoleHistoryRepository.save(history);
    }

    private void recordAssignmentHistory(Long userId, String fieldName, String oldValue, String newValue, String actor) {
        UserAssignmentHistory history = new UserAssignmentHistory(null, userId, fieldName,
                oldValue, newValue, null, actor, LocalDateTime.now());
        assignmentHistoryRepository.save(history);
    }

    private void validateAssignment(Long organizationId, Long departmentId, Long positionId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "主组织不存在"));
        if (!"ACTIVE".equals(organization.status()) || "WAREHOUSE".equals(organization.orgType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "主组织必须启用且不能是仓库");
        }
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "主部门不存在"));
        if (!"ACTIVE".equals(department.status())
                || !organizationId.equals(department.organizationId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "主部门必须启用且属于所选主组织");
        }
        var position = positionRepository.findById(positionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "主岗位不存在"));
        if (!"ACTIVE".equals(position.status()) || !departmentId.equals(position.departmentId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "主岗位必须启用且属于所选主部门");
        }
    }

    private void requireVisible(DataScopeResolution scope, User user) {
        requireOrganization(scope, user.mainOrganizationId());
    }

    private void requireOrganization(DataScopeResolution scope, Long organizationId) {
        if (!scope.isAllScope() && !scope.coversOrganization(organizationId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private User saveUserSafely(User user) {
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户账号或工号已存在");
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.id(), user.username(), user.displayName(),
                user.employeeCode(), user.email(), user.phone(),
                user.status(),
                user.mainOrganizationId(), user.mainDepartmentId(), user.mainPositionId(),
                resolveOrgName(user.mainOrganizationId()),
                resolveDeptName(user.mainDepartmentId()),
                resolvePositionName(user.mainPositionId()),
                user.lastLoginAt(), user.mustChangePassword(), user.createdAt(),
                user.version(),
                userRepository.findRoleSummaries(user.id()));
    }

    private UserDetailResponse toUserDetailResponse(User user, List<String> roles,
                                                     List<AssignmentHistoryResponse> history) {
        return new UserDetailResponse(user.id(), user.username(), user.displayName(),
                user.employeeCode(), user.email(), user.phone(),
                user.status(),
                user.mainOrganizationId(), user.mainDepartmentId(), user.mainPositionId(),
                resolveOrgName(user.mainOrganizationId()),
                resolveDeptName(user.mainDepartmentId()),
                resolvePositionName(user.mainPositionId()),
                user.lastLoginAt(), user.mustChangePassword(), user.createdAt(),
                user.version(), roles,
                userRepository.findRoleSummaries(user.id()),
                history);
    }

    private String resolveOrgName(Long orgId) {
        if (orgId == null) return null;
        return organizationRepository.findById(orgId).map(o -> o.orgName()).orElse(null);
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) return null;
        return departmentRepository.findById(deptId).map(d -> d.deptName()).orElse(null);
    }

    private String resolvePositionName(Long positionId) {
        if (positionId == null) return null;
        return positionRepository.findById(positionId).map(p -> p.positionName()).orElse(null);
    }

    private String generateRandomPassword(int length) {
        byte[] bytes = new byte[Math.max(length, 8)];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SrmPrincipal principal) {
            return principal.userId();
        }
        return null;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String summary(User user) {
        return "username=" + user.username() + ",organizationId=" + user.mainOrganizationId()
                + ",departmentId=" + user.mainDepartmentId() + ",positionId=" + user.mainPositionId()
                + ",status=" + user.status() + ",mustChangePassword=" + user.mustChangePassword();
    }
}
