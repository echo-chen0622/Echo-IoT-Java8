package org.echoiot.server.service.install.update;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * RequiredArgsConstructor 会自动为类的所有 final 属性生成构造方法
 * Profile("install")  只有在安装的时候才会执行
 *
 * @author Echo
 */
@RequiredArgsConstructor
@Service
@Profile("install")
@Slf4j
public class DefaultCacheCleanupService implements CacheCleanupService {

    private final CacheManager cacheManager;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;


    /**
     * 使用 sql 脚本清理由于架构升级或数据更新而无法再反序列化的缓存。
     * 请参阅 SqlDatabaseUpgradeService and dataupgrage.sql
     * 发现更改了哪些表
     *
     * @param fromVersion 现版本
     */
    @Override
    @SuppressWarnings({"AlibabaSwitchStatement", "java:S128"})
    public void clearCache(String fromVersion) {
        //跨多版本升级时，case，来控制版本按顺序逐步升级，所以不要加break，否则会跳过后面的case
        switch (fromVersion) {
            case "1.0.0":
                log.info("Clearing cache to upgrade from version 1.0.0 to 1.0.1 ...");
                /*
                这里可以通过两种方式：1.清除所有缓存{@link clearAll()} 2.清除指定缓存{@link clearCacheByName(String)} 。通常情况下，建议根据实际情况选择清除指定缓存
                示例： clearCacheByName("devices");
                */
            case "1.0.1":
                log.info("Clearing cache to upgrade from version 1.0.1 to 1.0.2 ...");
            default:
                //不执行任何操作，因为缓存清理是可选的，序列号类没有改动时，不需要清理缓存
                break;
        }
    }

    /**
     * 清除指定缓存
     *
     * @param cacheName
     */
    void clearCacheByName(final String cacheName) {
        log.info("Clearing cache [{}]", cacheName);
        @Nullable Cache cache = cacheManager.getCache(cacheName);
        Objects.requireNonNull(cache, "指定缓存不存在：" + cacheName);
        cache.clear();
    }

    /**
     * 清除所有缓存
     */
    void clearAll() {
        if (redisTemplate.isPresent()) {
            //如果缓存是通过 redis 实现的,只需要清除 redis 中的缓存
            log.info("Flushing all caches");
            redisTemplate.get().execute((RedisCallback<Object>) connection -> {
                connection.flushAll();
                return null;
            });
            return;
        }
        //遍历，清除所有缓存
        cacheManager.getCacheNames().forEach(this::clearCacheByName);
    }
}
