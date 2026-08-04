package com.srm.system.domain.model;

public record PermissionCatalogItem(
        Long id, String permissionCode, String domainCode, String resourceCode,
        String actionCode, String description, Boolean enabled) {
}
