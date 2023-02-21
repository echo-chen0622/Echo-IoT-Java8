package org.echoiot.server.cache.ota;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.CacheConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
@RequiredArgsConstructor
public class CaffeineOtaPackageCache implements OtaPackageDataCache {

    @NotNull
    private final CacheManager cacheManager;

    @Override
    public byte[] get(@NotNull String key) {
        return get(key, 0, 0);
    }

    @Override
    public byte[] get(@NotNull String key, int chunkSize, int chunk) {
        @Nullable byte[] data = cacheManager.getCache(CacheConstants.OTA_PACKAGE_DATA_CACHE).get(key, byte[].class);

        if (chunkSize < 1) {
            return data;
        }

        if (data != null && data.length > 0) {
            int startIndex = chunkSize * chunk;

            int size = Math.min(data.length - startIndex, chunkSize);

            if (startIndex < data.length && size > 0) {
                @NotNull byte[] result = new byte[size];
                System.arraycopy(data, startIndex, result, 0, size);
                return result;
            }
        }
        return new byte[0];
    }

    @Override
    public void put(@NotNull String key, byte[] value) {
        cacheManager.getCache(CacheConstants.OTA_PACKAGE_DATA_CACHE).putIfAbsent(key, value);
    }

    @Override
    public void evict(@NotNull String key) {
        cacheManager.getCache(CacheConstants.OTA_PACKAGE_DATA_CACHE).evict(key);
    }
}
