package com.srm.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "srm.security")
public record SrmSecurityProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        List<String> allowedOrigins,
        String bootstrapAdminUsername,
        String bootstrapAdminPassword,
        String bootstrapViewerUsername,
        String bootstrapViewerPassword,
        boolean secureCookies) {

    public SrmSecurityProperties {
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(10) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}

