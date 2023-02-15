package org.thingsboard.server.cache;

import org.springframework.data.redis.serializer.SerializationException;
import org.thingsboard.server.common.data.FSTUtils;

public class TbFSTRedisSerializer<K, V> implements TbRedisSerializer<K, V> {

    @Override
    public byte[] serialize(V value) throws SerializationException {
        return FSTUtils.encode(value);
    }

    @Override
    public V deserialize(K key, byte[] bytes) throws SerializationException {
        return FSTUtils.decode(bytes);
    }
}
