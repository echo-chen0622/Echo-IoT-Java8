package org.echoiot.server.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class RedisTbCacheTransaction<K extends Serializable, V extends Serializable> implements TbCacheTransaction<K, V> {

    @NotNull
    private final RedisTbTransactionalCache<K, V> cache;
    @NotNull
    private final RedisConnection connection;

    @Override
    public void putIfAbsent(K key, V value) {
        cache.put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
    }

    @Override
    public boolean commit() {
        try {
            @NotNull var execResult = connection.exec();
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
