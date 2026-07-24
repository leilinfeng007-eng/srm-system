package com.srm.platform.navigation;

import java.util.List;

public record MenuNode(
        String menuCode,
        String label,
        String route,
        String componentKey,
        String permission,
        int sortOrder,
        List<MenuNode> children) {

    public MenuNode {
        children = List.copyOf(children);
    }
}
