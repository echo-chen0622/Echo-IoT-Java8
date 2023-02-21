package org.echoiot.server.dao.timeseries;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CassandraTsPartitionsCache {

    private final AsyncLoadingCache<CassandraPartitionCacheKey, Boolean> partitionsCache;

    public CassandraTsPartitionsCache(long maxCacheSize) {
        this.partitionsCache = Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .buildAsync(key -> {
                    throw new IllegalStateException("'get' methods calls are not supported!");
                });
    }

    public boolean has(@NotNull CassandraPartitionCacheKey key) {
        return partitionsCache.getIfPresent(key) != null;
    }

    public void put(@NotNull CassandraPartitionCacheKey key) {
        partitionsCache.put(key, CompletableFuture.completedFuture(true));
    }

    public void invalidate(@NotNull CassandraPartitionCacheKey key) {
        partitionsCache.synchronous().invalidate(key);
    }
}
