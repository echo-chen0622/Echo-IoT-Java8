package org.echoiot.server.cache.device;

import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.Device;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("DeviceCache")
public class DeviceRedisCache extends RedisTbTransactionalCache<DeviceCacheKey, Device> {

    public DeviceRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.DEVICE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
