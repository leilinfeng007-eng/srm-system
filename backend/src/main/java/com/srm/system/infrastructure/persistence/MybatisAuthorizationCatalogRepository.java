package com.srm.system.infrastructure.persistence;

import com.srm.system.domain.model.EffectivePermissionGrant;
import com.srm.system.domain.model.MenuCatalogItem;
import com.srm.system.domain.model.PermissionCatalogItem;
import com.srm.system.domain.repository.AuthorizationCatalogRepository;
import com.srm.system.infrastructure.persistence.mapper.AuthorizationCatalogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisAuthorizationCatalogRepository implements AuthorizationCatalogRepository {
    private final AuthorizationCatalogMapper mapper;

    public MybatisAuthorizationCatalogRepository(AuthorizationCatalogMapper mapper) {
        this.mapper = mapper;
    }

    @Override public List<PermissionCatalogItem> findPermissions(int offset, int limit) {
        return mapper.findPermissions(offset, limit);
    }
    @Override public long countPermissions() { return mapper.countPermissions(); }
    @Override public List<MenuCatalogItem> findEnabledMenus() { return mapper.findEnabledMenus(); }
    @Override public List<EffectivePermissionGrant> findEffectivePermissionGrants(Long userId) {
        return mapper.findEffectivePermissionGrants(userId);
    }
}
