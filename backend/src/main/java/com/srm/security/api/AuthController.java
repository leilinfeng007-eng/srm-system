package com.srm.security.api;

import com.srm.common.api.ApiResponse;
import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import com.srm.config.SrmSecurityProperties;
import com.srm.security.api.request.LoginRequest;
import com.srm.security.api.response.TokenResponse;
import com.srm.security.api.response.UserPrincipalResponse;
import com.srm.security.auth.AuthenticationService;
import com.srm.security.auth.IssuedTokens;
import com.srm.security.auth.SrmPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String REFRESH_COOKIE = "SRM_REFRESH";

    private final AuthenticationService authenticationService;
    private final SrmSecurityProperties properties;

    public AuthController(
            AuthenticationService authenticationService,
            SrmSecurityProperties properties) {
        this.authenticationService = authenticationService;
        this.properties = properties;
    }

    @PostMapping("/login")
    @Operation(summary = "内部用户登录")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        IssuedTokens issued = authenticationService.login(
                request.username(),
                request.password(),
                clientAddress(servletRequest),
                servletRequest.getHeader(HttpHeaders.USER_AGENT));
        return tokenResponse(issued);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(HttpServletRequest request) {
        IssuedTokens issued = authenticationService.refresh(requireRefreshCookie(request));
        return tokenResponse(issued);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出当前会话")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal SrmPrincipal principal,
            HttpServletRequest request) {
        authenticationService.logout(principal, findRefreshCookie(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .body(ApiResponse.success());
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前内部用户")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<UserPrincipalResponse> me(@AuthenticationPrincipal SrmPrincipal principal) {
        return ApiResponse.success(UserPrincipalResponse.from(principal));
    }

    private ResponseEntity<ApiResponse<TokenResponse>> tokenResponse(IssuedTokens issued) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(issued.refreshToken()).toString())
                .body(ApiResponse.success(issued.response()));
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(properties.refreshTokenTtl())
                .build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String requireRefreshCookie(HttpServletRequest request) {
        String token = findRefreshCookie(request);
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return token;
    }

    private String findRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
