package org.echoiot.server.cache;

import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

public interface TbRedisSerializer<K, T> {

    @org.jetbrains.annotations.Nullable
    @Nullable
    byte[] serialize(@Nullable T t) throws SerializationException;

    @org.jetbrains.annotations.Nullable
    @Nullable
    T deserialize(K key, @Nullable byte[] bytes) throws SerializationException;

}
