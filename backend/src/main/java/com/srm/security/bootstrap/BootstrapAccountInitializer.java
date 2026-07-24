package com.srm.security.bootstrap;

import com.srm.config.SrmSecurityProperties;
import com.srm.config.SensitiveValuePolicy;
import com.srm.security.auth.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAccountInitializer implements ApplicationRunner {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SrmSecurityProperties properties;

    public BootstrapAccountInitializer(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            SrmSecurityProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validate("SRM_BOOTSTRAP_ADMIN_USERNAME", properties.bootstrapAdminUsername(), 3);
        SensitiveValuePolicy.requirePassword(
                "SRM_BOOTSTRAP_ADMIN_PASSWORD", properties.bootstrapAdminPassword(), 12);
        validate("SRM_BOOTSTRAP_VIEWER_USERNAME", properties.bootstrapViewerUsername(), 3);
        SensitiveValuePolicy.requirePassword(
                "SRM_BOOTSTRAP_VIEWER_PASSWORD", properties.bootstrapViewerPassword(), 12);
        SensitiveValuePolicy.rejectMatchingPasswords(
                "SRM_BOOTSTRAP_ADMIN_PASSWORD",
                properties.bootstrapAdminPassword(),
                "SRM_BOOTSTRAP_VIEWER_PASSWORD",
                properties.bootstrapViewerPassword());
        if (properties.bootstrapAdminUsername().equals(properties.bootstrapViewerUsername())) {
            throw new IllegalStateException("Bootstrap admin and viewer usernames must differ");
        }

        long adminId = users.createUserIfMissing(
                properties.bootstrapAdminUsername(),
                passwordEncoder.encode(properties.bootstrapAdminPassword()),
                "超级管理员",
                "bootstrap");
        users.assignRoleIfMissing(adminId, "SUPER_ADMIN", "bootstrap");

        long viewerId = users.createUserIfMissing(
                properties.bootstrapViewerUsername(),
                passwordEncoder.encode(properties.bootstrapViewerPassword()),
                "骨架查看者",
                "bootstrap");
        users.assignRoleIfMissing(viewerId, "SKELETON_VIEWER", "bootstrap");
    }

    private void validate(String name, String value, int minimumLength) {
        if (value == null || value.isBlank() || value.length() < minimumLength) {
            throw new IllegalStateException(name + " must contain at least " + minimumLength + " characters");
        }
    }
}
