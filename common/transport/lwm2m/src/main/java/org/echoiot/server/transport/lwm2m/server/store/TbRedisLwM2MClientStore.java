package org.echoiot.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.lwm2m.server.client.LwM2MClientState;
import org.echoiot.server.transport.lwm2m.server.store.util.LwM2MClientSerDes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class TbRedisLwM2MClientStore implements TbLwM2MClientStore {

    private static final String CLIENT_EP = "CLIENT#EP#";
    private final RedisConnectionFactory connectionFactory;

    public TbRedisLwM2MClientStore(RedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = redisConnectionFactory;
    }

    @Nullable
    @Override
    public LwM2mClient get(String endpoint) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            @Nullable byte[] data = connection.get(getKey(endpoint));
            if (data == null) {
                return null;
            } else {
                return LwM2MClientSerDes.deserialize(data);
            }
        }
    }

    @NotNull
    @Override
    public Set<LwM2mClient> getAll() {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            @NotNull Set<LwM2mClient> clients = new HashSet<>();
            @NotNull ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(CLIENT_EP + "*").build();
            @NotNull List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    @Nullable byte[] element = connection.get(key);
                    clients.add(LwM2MClientSerDes.deserialize(element));
                });
            });
            return clients;
        }
    }

    @Override
    public void put(@NotNull LwM2mClient client) {
        if (client.getState().equals(LwM2MClientState.UNREGISTERED)) {
            log.error("[{}] Client is in invalid state: {}!", client.getEndpoint(), client.getState(), new Exception());
        } else {
            @NotNull byte[] clientSerialized = LwM2MClientSerDes.serialize(client);
            try (@NotNull var connection = connectionFactory.getConnection()) {
                connection.getSet(getKey(client.getEndpoint()), clientSerialized);
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        try (@NotNull var connection = connectionFactory.getConnection()) {
            connection.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (CLIENT_EP + endpoint).getBytes();
    }
}
