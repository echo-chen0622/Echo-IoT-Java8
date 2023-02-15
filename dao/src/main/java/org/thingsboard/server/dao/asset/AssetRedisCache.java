package org.thingsboard.server.dao.asset;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AssetCache")
public class AssetRedisCache extends RedisTbTransactionalCache<AssetCacheKey, Asset> {

    public AssetRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ASSET_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
