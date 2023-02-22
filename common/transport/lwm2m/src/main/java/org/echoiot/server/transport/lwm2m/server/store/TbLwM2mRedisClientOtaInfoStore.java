package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;
import org.echoiot.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.echoiot.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class TbLwM2mRedisClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {
    private static final String OTA_EP = "OTA#EP#";

    private final RedisConnectionFactory connectionFactory;

    public TbLwM2mRedisClientOtaInfoStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private void put(OtaPackageType type, LwM2MClientOtaInfo<?, ?, ?> info) {
        try (var connection = connectionFactory.getConnection()) {
            connection.set((OTA_EP + type + info.getEndpoint()).getBytes(), JacksonUtil.toString(info).getBytes());
        }
    }

    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.FIRMWARE, endpoint, LwM2MClientFwOtaInfo.class);
    }

    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {
        put(OtaPackageType.FIRMWARE, info);
    }

    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.SOFTWARE, endpoint, LwM2MClientSwOtaInfo.class);
    }

    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {
        put(OtaPackageType.SOFTWARE, info);
    }

    @Nullable
    private <T extends LwM2MClientOtaInfo<?, ?, ?>> T getLwM2MClientOtaInfo(OtaPackageType type, String endpoint, Class<T> clazz) {
        try (var connection = connectionFactory.getConnection()) {
            @Nullable byte[] data = connection.get((OTA_EP + type + endpoint).getBytes());
            return JacksonUtil.fromBytes(data, clazz);
        }
    }
}
