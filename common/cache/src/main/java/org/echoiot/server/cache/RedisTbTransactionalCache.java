package org.echoiot.server.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class RedisTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    @Nullable
    private static final byte[] BINARY_NULL_VALUE = RedisSerializer.java().serialize(NullValue.INSTANCE);
    static final JedisPool MOCK_POOL = new JedisPool(); //non-null pool required for JedisConnection to trigger closing jedis connection

    @Getter
    private final String cacheName;
    private final JedisConnectionFactory connectionFactory;
    private final RedisSerializer<String> keySerializer = StringRedisSerializer.UTF_8;
    private final TbRedisSerializer<K, V> valueSerializer;
    @NotNull
    private final Expiration evictExpiration;
    @NotNull
    private final Expiration cacheTtl;

    public RedisTbTransactionalCache(String cacheName,
                                     CacheSpecsMap cacheSpecsMap,
                                     RedisConnectionFactory connectionFactory,
                                     @NotNull TBRedisCacheConfiguration configuration,
                                     TbRedisSerializer<K, V> valueSerializer) {
        this.cacheName = cacheName;
        this.connectionFactory = (JedisConnectionFactory) connectionFactory;
        this.valueSerializer = valueSerializer;
        this.evictExpiration = Expiration.from(configuration.getEvictTtlInMs(), TimeUnit.MILLISECONDS);
        this.cacheTtl = Optional.ofNullable(cacheSpecsMap)
                .map(CacheSpecsMap::getSpecs)
                .map(x -> x.get(cacheName))
                .map(CacheSpecs::getTimeToLiveInMinutes)
                .map(t -> Expiration.from(t, TimeUnit.MINUTES))
                .orElseGet(Expiration::persistent);
    }

    @Nullable
    @Override
    public TbCacheValueWrapper<V> get(@NotNull K key) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            @NotNull byte[] rawKey = getRawKey(key);
            @Nullable byte[] rawValue = connection.get(rawKey);
            if (rawValue == null) {
                return null;
            } else if (Arrays.equals(rawValue, BINARY_NULL_VALUE)) {
                return SimpleTbCacheValueWrapper.empty();
            } else {
                @Nullable V value = valueSerializer.deserialize(key, rawValue);
                return SimpleTbCacheValueWrapper.wrap(value);
            }
        }
    }

    @Override
    public void put(@NotNull K key, V value) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
        }
    }

    @Override
    public void putIfAbsent(@NotNull K key, V value) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            put(connection, key, value, RedisStringCommands.SetOption.SET_IF_ABSENT);
        }
    }

    @Override
    public void evict(@NotNull K key) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            connection.del(getRawKey(key));
        }
    }

    @Override
    public void evict(@NotNull Collection<K> keys) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            connection.del(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        }
    }

    @Override
    public void evictOrPut(@NotNull K key, V value) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            @NotNull var rawKey = getRawKey(key);
            @Nullable var records = connection.del(rawKey);
            if (records == null || records == 0) {
                //We need to put the value in case of Redis, because evict will NOT cancel concurrent transaction used to "get" the missing value from cache.
                connection.set(rawKey, getRawValue(value), evictExpiration, RedisStringCommands.SetOption.UPSERT);
            }
        }
    }

    @NotNull
    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(@NotNull K key) {
        @NotNull byte[][] rawKey = new byte[][]{getRawKey(key)};
        @NotNull RedisConnection connection = watch(rawKey);
        return new RedisTbCacheTransaction<>(this, connection);
    }

    @NotNull
    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(@NotNull List<K> keys) {
        @NotNull RedisConnection connection = watch(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        return new RedisTbCacheTransaction<>(this, connection);
    }

    @NotNull
    private RedisConnection getConnection(@NotNull byte[] rawKey) {
        if (!connectionFactory.isRedisClusterAware()) {
            return connectionFactory.getConnection();
        }
        @NotNull RedisConnection connection = connectionFactory.getClusterConnection();

        int slotNum = JedisClusterCRC16.getSlot(rawKey);
        Jedis jedis = ((JedisClusterConnection) connection).getNativeConnection().getConnectionFromSlot(slotNum);

        @NotNull JedisConnection jedisConnection = new JedisConnection(jedis, MOCK_POOL, jedis.getDB());
        jedisConnection.setConvertPipelineAndTxResults(connectionFactory.getConvertPipelineAndTxResults());

        return jedisConnection;
    }

    @NotNull
    private RedisConnection watch(@NotNull byte[][] rawKeysList) {
        @NotNull RedisConnection connection = getConnection(rawKeysList[0]);
        try {
            connection.watch(rawKeysList);
            connection.multi();
        } catch (Exception e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    private byte[] getRawKey(@NotNull K key) {
        @NotNull String keyString = cacheName + key;
        @Nullable byte[] rawKey;
        try {
            rawKey = keySerializer.serialize(keyString);
        } catch (Exception e) {
            log.warn("Failed to serialize the cache key: {}", key, e);
            throw new RuntimeException(e);
        }
        if (rawKey == null) {
            log.warn("Failed to serialize the cache key: {}", key);
            throw new IllegalArgumentException("Failed to serialize the cache key!");
        }
        return rawKey;
    }

    private byte[] getRawValue(@Nullable V value) {
        if (value == null) {
            return BINARY_NULL_VALUE;
        } else {
            try {
                return valueSerializer.serialize(value);
            } catch (Exception e) {
                log.warn("Failed to serialize the cache value: {}", value, e);
                throw new RuntimeException(e);
            }
        }
    }

    public void put(@NotNull RedisConnection connection, @NotNull K key, V value, @NotNull RedisStringCommands.SetOption setOption) {
        @NotNull byte[] rawKey = getRawKey(key);
        byte[] rawValue = getRawValue(value);
        connection.set(rawKey, rawValue, cacheTtl, setOption);
    }

}
