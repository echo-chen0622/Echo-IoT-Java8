package org.echoiot.server.cache;

import org.echoiot.server.common.data.FSTUtils;
import org.springframework.data.redis.serializer.SerializationException;

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
