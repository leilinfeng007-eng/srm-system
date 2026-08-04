package com.srm.system.domain.repository;

import com.srm.system.domain.model.EffectivePermissionGrant;
import com.srm.system.domain.model.MenuCatalogItem;
import com.srm.system.domain.model.PermissionCatalogItem;
import java.util.List;

public interface AuthorizationCatalogRepository {
    List<PermissionCatalogItem> findPermissions(int offset, int limit);
    long countPermissions();
    List<MenuCatalogItem> findEnabledMenus();
    List<EffectivePermissionGrant> findEffectivePermissionGrants(Long userId);
}
