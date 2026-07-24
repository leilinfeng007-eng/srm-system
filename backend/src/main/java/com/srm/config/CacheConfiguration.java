package com.srm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {

    public static final long NAVIGATION_MAXIMUM_SIZE = 256;
    public static final Duration NAVIGATION_TTL = Duration.ofMinutes(5);

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(CacheNames.NAVIGATION_MENUS));
        manager.setAllowNullValues(false);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(NAVIGATION_MAXIMUM_SIZE)
                .expireAfterWrite(NAVIGATION_TTL)
                .recordStats());
        return manager;
    }
}
