package com.srm.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

public class SensitiveConfigurationInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        String runtime = property(environment, "srm.environment");
        String jwtSecret = property(environment, "srm.security.jwt-secret");
        String adminPassword = property(environment, "srm.security.bootstrap-admin-password");
        String viewerPassword = property(environment, "srm.security.bootstrap-viewer-password");

        SensitiveValuePolicy.requireSecret("SRM_JWT_SECRET", jwtSecret, 32);
        SensitiveValuePolicy.requirePassword("SRM_BOOTSTRAP_ADMIN_PASSWORD", adminPassword, 12);
        SensitiveValuePolicy.requirePassword("SRM_BOOTSTRAP_VIEWER_PASSWORD", viewerPassword, 12);
        SensitiveValuePolicy.rejectMatchingPasswords(
                "SRM_BOOTSTRAP_ADMIN_PASSWORD",
                adminPassword,
                "SRM_BOOTSTRAP_VIEWER_PASSWORD",
                viewerPassword);

        if (!"test".equalsIgnoreCase(runtime)) {
            SensitiveValuePolicy.requireSecret(
                    "SRM_DB_PASSWORD",
                    property(environment, "spring.datasource.password"),
                    12);
        }
        if ("production".equalsIgnoreCase(runtime)
                && !environment.getProperty("srm.security.secure-cookies", Boolean.class, false)) {
            throw new IllegalStateException("SRM_SECURE_COOKIES must be true in production");
        }
    }

    private String property(Environment environment, String name) {
        try {
            return environment.getProperty(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
