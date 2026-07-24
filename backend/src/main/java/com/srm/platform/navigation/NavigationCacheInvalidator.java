package com.srm.platform.navigation;

import com.srm.config.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class NavigationCacheInvalidator {

    @CacheEvict(cacheNames = CacheNames.NAVIGATION_MENUS, key = "#userId")
    public void evictUser(long userId) {
        // Annotation-driven invalidation keeps callers independent of Caffeine APIs.
    }

    @CacheEvict(cacheNames = CacheNames.NAVIGATION_MENUS, allEntries = true)
    public void evictAll() {
        // Reserved for menu/role-permission maintenance that affects multiple users.
    }
}
