package com.srm.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR("PLATFORM_VALIDATION_ERROR", "Request validation failed", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_REQUIRED("PLATFORM_AUTHENTICATION_REQUIRED", "Authentication is required", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("PLATFORM_INVALID_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("PLATFORM_TOKEN_INVALID", "Token is invalid or expired", HttpStatus.UNAUTHORIZED),
    MUST_CHANGE_PASSWORD("PLATFORM_MUST_CHANGE_PASSWORD", "Initial password must be changed", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("PLATFORM_ACCESS_DENIED", "Access is denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("PLATFORM_RESOURCE_NOT_FOUND", "Resource was not found", HttpStatus.NOT_FOUND),
    CONFLICT("PLATFORM_CONFLICT", "The request conflicts with current state", HttpStatus.CONFLICT),
    INTERNAL_ERROR("PLATFORM_INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }
}

