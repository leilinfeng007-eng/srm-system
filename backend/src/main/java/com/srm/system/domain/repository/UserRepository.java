package com.srm.system.domain.repository;

import com.srm.system.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    User save(User user);

    User update(User user);

    long countByUsername(String username);

    List<String> findActiveRoleCodes(Long userId);

    List<User> findPage(String username, String displayName, String status,
                        List<Long> allowedOrganizationIds, boolean allOrganizations,
                        int offset, int limit);

    long count(String username, String displayName, String status,
               List<Long> allowedOrganizationIds, boolean allOrganizations);

    boolean updatePassword(Long userId, String passwordHash, boolean mustChange, String actor);

    boolean updateStatus(Long userId, String status);

    void revokeSessions(Long userId);
}
