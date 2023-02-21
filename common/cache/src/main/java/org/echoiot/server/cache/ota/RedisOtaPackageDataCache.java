package org.echoiot.server.cache.ota;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.CacheConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisOtaPackageDataCache implements OtaPackageDataCache {

    @NotNull
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public byte[] get(String key) {
        return get(key, 0, 0);
    }

    @Override
    public byte[] get(String key, int chunkSize, int chunk) {
        try (@NotNull RedisConnection connection = redisConnectionFactory.getConnection()) {
            if (chunkSize == 0) {
                return connection.get(toOtaPackageCacheKey(key));
            }

            int startIndex = chunkSize * chunk;
            int endIndex = startIndex + chunkSize - 1;
            return connection.getRange(toOtaPackageCacheKey(key), startIndex, endIndex);
        }
    }

    @Override
    public void put(String key, @NotNull byte[] value) {
        try (@NotNull RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.set(toOtaPackageCacheKey(key), value);
        }
    }

    @Override
    public void evict(String key) {
        try (@NotNull RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(toOtaPackageCacheKey(key));
        }
    }

    private byte[] toOtaPackageCacheKey(String key) {
        return String.format("%s::%s", CacheConstants.OTA_PACKAGE_DATA_CACHE, key).getBytes();
    }
}
