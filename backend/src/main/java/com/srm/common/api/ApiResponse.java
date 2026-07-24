package com.srm.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.srm.common.web.TraceContext;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "success", data, TraceContext.currentTraceId(), Instant.now());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(String code, String message, T details) {
        return new ApiResponse<>(code, message, details, TraceContext.currentTraceId(), Instant.now());
    }
}

