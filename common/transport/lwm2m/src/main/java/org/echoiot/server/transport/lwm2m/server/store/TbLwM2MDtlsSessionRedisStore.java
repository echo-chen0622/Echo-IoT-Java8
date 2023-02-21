package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class TbLwM2MDtlsSessionRedisStore implements TbLwM2MDtlsSessionStore {

    private static final String SESSION_EP = "SESSION#EP#";
    private final RedisConnectionFactory connectionFactory;
    private final FSTConfiguration serializer;

    public TbLwM2MDtlsSessionRedisStore(RedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = redisConnectionFactory;
        this.serializer = FSTConfiguration.createDefaultConfiguration();
    }

    @Override
    public void put(String endpoint, TbX509DtlsSessionInfo msg) {
        try (@NotNull var c = connectionFactory.getConnection()) {
            var serializedMsg = serializer.asByteArray(msg);
            if (serializedMsg != null) {
                c.set(getKey(endpoint), serializedMsg);
            } else {
                throw new RuntimeException("Problem with serialization of message: " + msg);
            }
        }
    }

    @Nullable
    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        try (@NotNull var c = connectionFactory.getConnection()) {
            @Nullable var data = c.get(getKey(endpoint));
            if (data != null) {
                return (TbX509DtlsSessionInfo) serializer.asObject(data);
            } else {
                return null;
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        try (@NotNull var c = connectionFactory.getConnection()) {
            c.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (SESSION_EP + endpoint).getBytes();
    }
}
