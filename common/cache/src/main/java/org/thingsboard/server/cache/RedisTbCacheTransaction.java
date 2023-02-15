package org.thingsboard.server.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class RedisTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {

    private final RedisTbTransactionalCache<K, V> cache;
    private final RedisConnection connection;

    @Override
    public void putIfAbsent(K key, V value) {
        cache.put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
    }

    @Override
    public boolean commit() {
        try {
            var execResult = connection.exec();
            var result = execResult != null && execResult.stream().anyMatch(Objects::nonNull);
            return result;
        } finally {
            connection.close();
        }
    }

    @Override
    public void rollback() {
        try {
            connection.discard();
        } finally {
            connection.close();
        }
    }

}
