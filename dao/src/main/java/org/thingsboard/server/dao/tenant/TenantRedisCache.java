package org.thingsboard.server.dao.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbFSTRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TenantCache")
public class TenantRedisCache extends RedisTbTransactionalCache<TenantId, Tenant> {

    public TenantRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TENANTS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
