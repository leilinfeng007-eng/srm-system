package com.srm.system.domain.repository;

import com.srm.system.domain.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findById(Long id);

    Role save(Role role);

    Role update(Role role);

    boolean updateStatus(Long id, String status, Long version);

    boolean existsByRoleCode(String roleCode);

    long countActiveUsers(Long roleId);

    long countActiveUsersExcluding(Long roleId, Long excludeUserId);

    void lockActiveAssignments(Long roleId);

    List<Long> findUserIdsByRole(Long roleId);

    List<Long> findMenuIdsByRole(Long roleId);

    List<Long> findPermissionIdsByRole(Long roleId);

    void assignUserToRole(Long userId, Long roleId, String actor);

    void removeUserFromRole(Long userId, Long roleId);

    List<Long> findRoleIdsByUserId(Long userId);

    List<String> findPermissionCodesByRole(Long roleId);

    List<Role> findPage(int offset, int limit);

    long countAll();

    void clearRoleMenus(Long roleId, String actor);

    void clearRolePermissions(Long roleId, String actor);

    void addRoleMenus(Long roleId, List<Long> menuIds, String actor);

    void addRolePermissions(Long roleId, List<Long> permissionIds, String actor);

    void replaceDataPolicies(Long roleId, List<DataPolicyRequest> policies, String actor);

    List<DataPolicyRequest> findDataPolicies(Long roleId);

    long countUsersLosingAllRoles(Long roleId);
}
