package org.echoiot.server.dao.asset;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AssetCache")
public class AssetRedisCache extends RedisTbTransactionalCache<AssetCacheKey, Asset> {

    public AssetRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ASSET_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
