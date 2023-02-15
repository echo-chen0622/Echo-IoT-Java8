package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class TbRedisLwM2MModelConfigStore implements TbLwM2MModelConfigStore {
    private static final String MODEL_EP = "MODEL#EP#";
    private final RedisConnectionFactory connectionFactory;

    @Override
    public List<LwM2MModelConfig> getAll() {
        try (var connection = connectionFactory.getConnection()) {
            List<LwM2MModelConfig> configs = new ArrayList<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(MODEL_EP + "*").build();
            List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    byte[] element = connection.get(key);
                    configs.add(JacksonUtil.fromBytes(element, LwM2MModelConfig.class));
                });
            });
            return configs;
        }
    }

    @Override
    public void put(LwM2MModelConfig modelConfig) {
        byte[] clientSerialized = JacksonUtil.writeValueAsBytes(modelConfig);
        try (var connection = connectionFactory.getConnection()) {
            connection.getSet(getKey(modelConfig.getEndpoint()), clientSerialized);
        }
    }

    @Override
    public void remove(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (MODEL_EP + endpoint).getBytes();
    }

}
