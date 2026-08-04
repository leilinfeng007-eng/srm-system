package com.srm.system.domain.model;

public record MenuCatalogItem(
        Long id, Long parentId, String menuCode, String label, String route,
        String componentKey, String permissionCode, Integer sortOrder) {
}
