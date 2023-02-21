package org.echoiot.server.dao.ota;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbFSTRedisSerializer;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.cache.RedisTbTransactionalCache;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("OtaPackageCache")
public class OtaPackageRedisCache extends RedisTbTransactionalCache<OtaPackageCacheKey, OtaPackageInfo> {

    public OtaPackageRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.OTA_PACKAGE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbFSTRedisSerializer<>());
    }
}
