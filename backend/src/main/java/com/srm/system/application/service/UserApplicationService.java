package com.srm.system.application.service;

import com.srm.common.api.PageResult;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.system.api.request.CreateUserRequest;
import com.srm.system.api.request.UpdateUserRequest;
import com.srm.system.api.response.AssignmentHistoryResponse;
import com.srm.system.api.response.ResetPasswordResponse;
import com.srm.system.api.response.UserDetailResponse;
import com.srm.system.api.response.UserResponse;
import com.srm.system.domain.model.User;
import com.srm.system.domain.model.UserAssignmentHistory;
import com.srm.system.domain.repository.DepartmentRepository;
import com.srm.system.domain.repository.PositionRepository;
import com.srm.system.domain.repository.UserAssignmentHistoryRepository;
import com.srm.system.domain.repository.UserRepository;
import com.srm.system.domain.permission.DataScopeAuthorizationService;
import com.srm.system.domain.permission.DataScopeResolution;
import com.srm.system.domain.service.AuditRecorder;
import com.srm.masterdata.domain.repository.OrganizationRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserApplicationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserAssignmentHistoryRepository assignmentHistoryRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataScopeAuthorizationService dataScope;
    private final AuditRecorder audit;

    public UserApplicationService(UserRepository userRepository,
                                  UserAssignmentHistoryRepository assignmentHistoryRepository,
                                  OrganizationRepository organizationRepository,
                                  DepartmentRepository departmentRepository,
                                  PositionRepository positionRepository,
                                  PasswordEncoder passwordEncoder,
                                  DataScopeAuthorizationService dataScope,
                                  AuditRecorder audit) {
        this.userRepository = userRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
        this.organizationRepository = organizationRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.passwordEncoder = passwordEncoder;
        this.dataScope = dataScope;
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
            throw new BusinessException(ErrorCode.CONFLICT, "Username already exists");
        }
        requireOrganization(dataScope.requireWrite(
                "system:user:create", "system", "ORGANIZATION"), req.mainOrganizationId());
        validateAssignment(req.mainOrganizationId(), req.mainDepartmentId(), req.mainPositionId());
        Instant now = Instant.now();
        String passwordHash = passwordEncoder.encode(req.password());
        User user = new User(null, req.username(), passwordHash, req.displayName(),
                req.employeeCode(), req.email(), req.phone(),
                req.mainOrganizationId(), req.mainDepartmentId(), req.mainPositionId(),
                true, "ACTIVE", null, now, actor, actor, now, 0L);
        User saved = userRepository.save(user);
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
                req.version() != null ? req.version() : existing.version());
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
            throw new BusinessException(ErrorCode.CONFLICT, "User has been modified by another operation");
        }
        userRepository.updateStatus(id, "ACTIVE");
        audit.record("USER_ENABLED", "USER", String.valueOf(id), "SUCCESS",
                "status="+existing.status(), "status=ACTIVE", null);
    }

    @Transactional
    public void disableUser(Long id, Long version) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        requireVisible(dataScope.requireWrite(
                "system:user:disable", "system", "ORGANIZATION"), existing);
        if (version != null && !version.equals(existing.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, "User has been modified by another operation");
        }
        userRepository.updateStatus(id, "DISABLED");
        userRepository.revokeSessions(id);
        audit.record("USER_DISABLED", "USER", String.valueOf(id), "SUCCESS",
                "status="+existing.status(), "status=DISABLED,sessions=revoked", null);
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
        audit.record("USER_PASSWORD_RESET", "USER", String.valueOf(id), "SUCCESS",
                null, "mustChangePassword=true", null);
        return new ResetPasswordResponse(tempPassword);
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

    private void recordAssignmentHistory(Long userId, String fieldName, String oldValue, String newValue, String actor) {
        UserAssignmentHistory history = new UserAssignmentHistory(null, userId, fieldName,
                oldValue, newValue, null, actor, LocalDateTime.now());
        assignmentHistoryRepository.save(history);
    }

    private void validateAssignment(Long organizationId, Long departmentId, Long positionId) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Main organization does not exist"));
        if (!"ACTIVE".equals(organization.status()) || "WAREHOUSE".equals(organization.orgType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Main organization must be active and cannot be a warehouse");
        }
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Main department does not exist"));
        if (!"ACTIVE".equals(department.status())
                || !organizationId.equals(department.organizationId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Main department must be active and belong to the main organization");
        }
        var position = positionRepository.findById(positionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Main position does not exist"));
        if (!"ACTIVE".equals(position.status()) || !departmentId.equals(position.departmentId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Main position must be active and belong to the main department");
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

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.id(), user.username(), user.displayName(),
                user.employeeCode(), user.email(), user.phone(),
                user.status(),
                user.mainOrganizationId(), user.mainDepartmentId(), user.mainPositionId(),
                resolveOrgName(user.mainOrganizationId()),
                resolveDeptName(user.mainDepartmentId()),
                resolvePositionName(user.mainPositionId()),
                user.lastLoginAt(), user.mustChangePassword(), user.createdAt());
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
                roles, history);
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

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String summary(User user){return "username="+user.username()+",organizationId="+user.mainOrganizationId()+",departmentId="+user.mainDepartmentId()+",positionId="+user.mainPositionId()+",status="+user.status()+",mustChangePassword="+user.mustChangePassword();}
}
