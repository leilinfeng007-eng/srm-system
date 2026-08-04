package com.srm.system.domain.repository;

public record DataPolicyRequest(
        String domainCode,
        String dimensionCode,
        String scopeType,
        boolean includeChildren,
        String operationMode) {
}
