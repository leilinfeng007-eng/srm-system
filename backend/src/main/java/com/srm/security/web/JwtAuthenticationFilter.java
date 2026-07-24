package com.srm.security.web;

import com.srm.common.exception.ErrorCode;
import com.srm.security.auth.SrmPrincipal;
import com.srm.security.auth.UserAccountRepository;
import com.srm.security.token.JwtClaims;
import com.srm.security.token.JwtTokenService;
import com.srm.security.token.TokenSessionRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;
    private final TokenSessionRepository sessions;
    private final UserAccountRepository users;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(
            JwtTokenService tokenService,
            TokenSessionRepository sessions,
            UserAccountRepository users,
            SecurityErrorWriter errorWriter) {
        this.tokenService = tokenService;
        this.sessions = sessions;
        this.users = users;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = tokenService.parseAccessToken(authorization.substring(7));
            if (!sessions.isAccessActive(
                    claims.sessionId(), claims.userId(), claims.jti(), Instant.now())) {
                errorWriter.write(response, ErrorCode.TOKEN_INVALID);
                return;
            }
            SrmPrincipal principal = users.findById(claims.userId())
                    .filter(SrmPrincipal::isEnabled)
                    .orElseThrow(() -> new IllegalArgumentException("User is not active"))
                    .withSessionId(claims.sessionId());
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(response, ErrorCode.TOKEN_INVALID);
        }
    }
}

