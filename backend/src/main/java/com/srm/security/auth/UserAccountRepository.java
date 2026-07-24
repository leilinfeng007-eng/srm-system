package com.srm.security.auth;

import java.time.Instant;
import java.util.Optional;

public interface UserAccountRepository {

    Optional<SrmPrincipal> findByUsername(String username);

    Optional<SrmPrincipal> findById(long userId);

    long createUserIfMissing(String username, String passwordHash, String displayName, String actor);

    void assignRoleIfMissing(long userId, String roleCode, String actor);

    void markLogin(long userId, Instant loginAt);
}
