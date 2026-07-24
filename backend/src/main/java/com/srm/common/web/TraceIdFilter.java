package com.srm.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{8,64}");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestedTraceId = request.getHeader(TRACE_HEADER);
        String traceId = requestedTraceId != null && SAFE_TRACE_ID.matcher(requestedTraceId).matches()
                ? requestedTraceId
                : generateTraceId();
        MDC.put(TraceContext.TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.TRACE_ID);
        }
    }

    private String generateTraceId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

