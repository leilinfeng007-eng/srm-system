package com.srm.workbench.domain.model;

public record WorkbenchBaseline(
        String moduleCode,
        BaselineState state,
        boolean databaseReachable) {

    public WorkbenchBaseline {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("moduleCode must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}
