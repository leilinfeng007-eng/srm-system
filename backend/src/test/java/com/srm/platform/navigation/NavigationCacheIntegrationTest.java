package com.srm.platform.navigation;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Cache;
import com.srm.config.CacheConfiguration;
import com.srm.config.CacheNames;
import com.srm.security.auth.UserAccountRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NavigationCacheIntegrationTest {

    @Autowired
    private NavigationQueryService navigation;

    @Autowired
    private NavigationCacheInvalidator invalidator;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cachesOnlyNavigationMetadataWithBoundedTtlAndExactUserEviction() {
        long userId = users.findByUsername("stage0_admin").orElseThrow().userId();
        CaffeineCache springCache = (CaffeineCache) cacheManager.getCache(CacheNames.NAVIGATION_MENUS);
        assertThat(springCache).isNotNull();
        Cache<Object, Object> nativeCache = springCache.getNativeCache();
        invalidator.evictUser(userId);

        long missesBefore = nativeCache.stats().missCount();
        long hitsBefore = nativeCache.stats().hitCount();
        assertThat(navigation.findGrantedTree(userId)).hasSize(12);
        assertThat(navigation.findGrantedTree(userId)).hasSize(12);

        assertThat(nativeCache.stats().missCount() - missesBefore).isEqualTo(1);
        assertThat(nativeCache.stats().hitCount() - hitsBefore).isEqualTo(1);
        assertThat(nativeCache.policy().eviction().orElseThrow().getMaximum())
                .isEqualTo(CacheConfiguration.NAVIGATION_MAXIMUM_SIZE);
        assertThat(nativeCache.policy().expireAfterWrite().orElseThrow()
                        .getExpiresAfter(TimeUnit.SECONDS))
                .isEqualTo(CacheConfiguration.NAVIGATION_TTL.toSeconds());

        invalidator.evictUser(userId);
        assertThat(nativeCache.getIfPresent(userId)).isNull();
    }
}
