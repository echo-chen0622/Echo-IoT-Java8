package org.echoiot.server.service.session;

import com.google.protobuf.InvalidProtocolBufferException;
import org.echoiot.server.cache.CacheSpecsMap;
import org.echoiot.server.cache.RedisTbTransactionalCache;
import org.echoiot.server.cache.TBRedisCacheConfiguration;
import org.echoiot.server.cache.TbRedisSerializer;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.gen.transport.TransportProtos;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("SessionCache")
public class SessionRedisCache extends RedisTbTransactionalCache<DeviceId, TransportProtos.DeviceSessionsCacheEntry> {

    public SessionRedisCache(@NotNull TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.SESSIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(@NotNull TransportProtos.DeviceSessionsCacheEntry deviceSessionsCacheEntry) throws SerializationException {
                return deviceSessionsCacheEntry.toByteArray();
            }

            @Override
            public TransportProtos.DeviceSessionsCacheEntry deserialize(DeviceId key, byte[] bytes) throws SerializationException {
                try {
                    return TransportProtos.DeviceSessionsCacheEntry.parseFrom(bytes);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException("Failed to deserialize session cache entry");
                }
            }
        });
    }
}
