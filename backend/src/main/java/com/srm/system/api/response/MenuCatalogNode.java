package com.srm.system.api.response;

import java.util.List;

public record MenuCatalogNode(
        Long id, String menuCode, String label, String route, String componentKey,
        String permissionCode, Integer sortOrder, List<MenuCatalogNode> children) {
    public MenuCatalogNode {
        children = List.copyOf(children);
    }
}
