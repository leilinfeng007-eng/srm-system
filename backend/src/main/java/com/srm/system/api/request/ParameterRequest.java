package com.srm.system.api.request;

public record ParameterRequest(String paramCode, String paramName, String paramType,
                               String defaultValue, String validationRule,
                               Boolean approvalRequired, String description) {}
