package com.srm.platform.meta;

public record ModuleMeta(
        String domainCode,
        String label,
        String backendPackage,
        String routePrefix,
        String permissionPrefix,
        int sortOrder,
        int phase,
        String status) {
}

