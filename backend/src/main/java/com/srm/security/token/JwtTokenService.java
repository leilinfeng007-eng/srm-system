package com.srm.security.token;

import com.srm.config.SrmSecurityProperties;
import com.srm.config.SensitiveValuePolicy;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SrmSecurityProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(SrmSecurityProperties properties) {
        this.properties = properties;
        String configuredSecret = SensitiveValuePolicy.requireSecret(
                "SRM_JWT_SECRET", properties.jwtSecret(), 32);
        this.signingKey = Keys.hmacShaKeyFor(configuredSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String issueAccessToken(
            long userId,
            long sessionId,
            String username,
            String jti,
            Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        return Jwts.builder()
                .subject(username)
                .id(jti)
                .claim("uid", userId)
                .claim("sid", sessionId)
                .claim("typ", "access")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public JwtClaims parseAccessToken(String token) {
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
        Claims claims = parsed.getPayload();
        if (!"access".equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        Number userId = claims.get("uid", Number.class);
        Number sessionId = claims.get("sid", Number.class);
        return new JwtClaims(
                userId.longValue(),
                sessionId.longValue(),
                claims.getSubject(),
                claims.getId(),
                claims.getExpiration().toInstant());
    }
}
