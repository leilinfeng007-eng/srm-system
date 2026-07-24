package com.srm.security.web;

import com.srm.common.api.ApiResponse;
import com.srm.common.exception.ErrorCode;
import com.srm.security.auth.SecurityAuditRepository;
import com.srm.security.auth.SrmPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityAccessDeniedHandler {

    private final SecurityAuditRepository audit;

    public SecurityAccessDeniedHandler(SecurityAuditRepository audit) {
        this.audit = audit;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            AccessDeniedException exception,
            HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SrmPrincipal principal) {
            String target = request.getMethod() + " " + request.getRequestURI();
            audit.recordOperation(
                    principal.userId(),
                    "SECURITY_ACCESS_DENIED",
                    "HTTP_ENDPOINT",
                    target.substring(0, Math.min(target.length(), 100)),
                    ErrorCode.ACCESS_DENIED.code(),
                    Instant.now());
        }
        ErrorCode error = ErrorCode.ACCESS_DENIED;
        return ResponseEntity.status(error.status())
                .body(ApiResponse.error(error.code(), error.defaultMessage(), null));
    }
}
