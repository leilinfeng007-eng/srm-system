package com.srm.platform.navigation;

import java.util.List;

public interface NavigationRepository {

    List<MenuNode> findGrantedTree(long userId);
}
