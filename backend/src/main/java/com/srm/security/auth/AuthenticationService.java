package com.srm.security.auth;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.config.SrmSecurityProperties;
import com.srm.security.api.response.TokenResponse;
import com.srm.security.api.response.UserPrincipalResponse;
import com.srm.security.token.JwtTokenService;
import com.srm.security.token.RefreshSession;
import com.srm.security.token.TokenSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountRepository users;
    private final TokenSessionRepository sessions;
    private final SecurityAuditRepository audit;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SrmSecurityProperties properties;
    private final Clock clock;

    @Autowired
    public AuthenticationService(
            UserAccountRepository users,
            TokenSessionRepository sessions,
            SecurityAuditRepository audit,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            SrmSecurityProperties properties) {
        this(users, sessions, audit, passwordEncoder, jwtTokenService, properties, Clock.systemUTC());
    }

    AuthenticationService(
            UserAccountRepository users,
            TokenSessionRepository sessions,
            SecurityAuditRepository audit,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            SrmSecurityProperties properties,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens login(
            String username,
            String password,
            String remoteAddress,
            String deviceInfo) {
        Instant now = clock.instant();
        Optional<SrmPrincipal> candidate = users.findByUsername(username);
        if (candidate.isEmpty()
                || !candidate.get().isEnabled()
                || !passwordEncoder.matches(password, candidate.get().password())) {
            audit.recordLogin(
                    candidate.map(SrmPrincipal::userId).orElse(null),
                    username,
                    false,
                    remoteAddress,
                    "INVALID_CREDENTIALS",
                    now);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        SrmPrincipal principal = candidate.get();
        String refreshToken = newOpaqueToken();
        String accessJti = newJti();
        long sessionId = sessions.create(
                principal.userId(),
                sha256(refreshToken),
                accessJti,
                now.plus(properties.refreshTokenTtl()),
                trimDeviceInfo(deviceInfo));
        SrmPrincipal sessionPrincipal = principal.withSessionId(sessionId);
        users.markLogin(principal.userId(), now);
        audit.recordLogin(
                principal.userId(), username, true, remoteAddress, "SUCCESS", now);
        return issue(sessionPrincipal, refreshToken, accessJti, now);
    }

    @Transactional
    public IssuedTokens refresh(String refreshToken) {
        Instant now = clock.instant();
        String previousHash = sha256(refreshToken);
        RefreshSession session = sessions.findByTokenHash(previousHash)
                .filter(value -> value.activeAt(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        SrmPrincipal principal = users.findById(session.userId())
                .filter(SrmPrincipal::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));

        String nextRefreshToken = newOpaqueToken();
        String accessJti = newJti();
        boolean rotated = sessions.rotate(
                session.id(),
                previousHash,
                sha256(nextRefreshToken),
                accessJti,
                now.plus(properties.refreshTokenTtl()),
                now);
        if (!rotated) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return issue(principal.withSessionId(session.id()), nextRefreshToken, accessJti, now);
    }

    @Transactional
    public void logout(SrmPrincipal principal, String refreshToken) {
        Instant now = clock.instant();
        if (principal != null && principal.sessionId() != null) {
            sessions.revokeById(principal.sessionId(), now);
            audit.recordOperation(
                    principal.userId(),
                    "AUTH_LOGOUT",
                    "REFRESH_SESSION",
                    Long.toString(principal.sessionId()),
                    "SUCCESS",
                    now);
        } else if (refreshToken != null && !refreshToken.isBlank()) {
            sessions.revokeByTokenHash(sha256(refreshToken), now);
        }
    }

    private IssuedTokens issue(
            SrmPrincipal principal,
            String refreshToken,
            String accessJti,
            Instant now) {
        String accessToken = jwtTokenService.issueAccessToken(
                principal.userId(),
                principal.sessionId(),
                principal.username(),
                accessJti,
                now);
        TokenResponse response = new TokenResponse(
                accessToken,
                "Bearer",
                properties.accessTokenTtl().toSeconds(),
                UserPrincipalResponse.from(principal));
        return new IssuedTokens(response, refreshToken);
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String newJti() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String trimDeviceInfo(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
