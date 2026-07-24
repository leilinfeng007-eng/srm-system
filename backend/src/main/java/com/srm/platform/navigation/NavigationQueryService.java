package com.srm.platform.navigation;

import com.srm.config.CacheNames;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavigationQueryService {

    private final NavigationRepository repository;

    public NavigationQueryService(NavigationRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = CacheNames.NAVIGATION_MENUS, key = "#userId", sync = true)
    @Transactional(readOnly = true)
    public List<MenuNode> findGrantedTree(long userId) {
        return repository.findGrantedTree(userId);
    }
}
