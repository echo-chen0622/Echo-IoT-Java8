package org.echoiot.server.dao.tenant;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TenantExistsCache")
public class TenantExistsRedisCache extends RedisTbTransactionalCache<TenantId, Boolean> {

    public TenantExistsRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TENANTS_EXIST_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
