package org.thingsboard.server.dao.entityview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("EntityViewCache")
public class EntityViewRedisCache extends RedisTbTransactionalCache<EntityViewCacheKey, EntityViewCacheValue> {

    public EntityViewRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ENTITY_VIEW_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
