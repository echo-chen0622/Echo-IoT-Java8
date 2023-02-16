package org.echoiot.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheSpecsMap.class, TbCaffeineCacheConfiguration.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "cache.type=caffeine",
        "cache.specs.relations.timeToLiveInMinutes=1440",
        "cache.specs.relations.maxSize=0",
        "cache.specs.devices.timeToLiveInMinutes=60",
        "cache.specs.devices.maxSize=100"})
@Slf4j
public class CacheSpecsMapTest {

    @Autowired
    CacheManager cacheManager;

    @Test
    public void verifyNotTransactionAwareCacheManagerProxy() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager).isInstanceOf(SimpleCacheManager.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyExistedCachesWithNoTransactionAwareCacheDecorator() {
        // We no longer use built-in transaction support for the caches, because we have our own cache cleanup and transaction logic that implements CAS.
        assertThat(cacheManager.getCache("relations")).isInstanceOf(CaffeineCache.class);
        assertThat(cacheManager.getCache("devices")).isInstanceOf(CaffeineCache.class);
    }

    @Test
    public void givenCacheConfig_whenCacheManagerReady_thenVerifyNonExistedCaches() {
        assertThat(cacheManager.getCache("rainbows_and_unicorns")).isNull();
    }
}
