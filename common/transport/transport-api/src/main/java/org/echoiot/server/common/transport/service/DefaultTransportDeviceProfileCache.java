package org.echoiot.server.common.transport.service;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.transport.TransportDeviceProfileCache;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbTransportComponent;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@TbTransportComponent
public class DefaultTransportDeviceProfileCache implements TransportDeviceProfileCache {

    private final Lock deviceProfileFetchLock = new ReentrantLock();
    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfiles = new ConcurrentHashMap<>();
    private final DataDecodingEncodingService dataDecodingEncodingService;

    private TransportService transportService;

    @Lazy
    @Autowired
    public void setTransportService(TransportService transportService) {
        this.transportService = transportService;
    }

    public DefaultTransportDeviceProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @Nullable
    @Override
    public DeviceProfile getOrCreate(DeviceProfileId id, ByteString profileBody) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
            if (deviceProfile.isPresent()) {
                profile = deviceProfile.get();
                deviceProfiles.put(id, profile);
            }
        }
        return profile;
    }

    @Override
    public DeviceProfile get(DeviceProfileId id) {
        return this.getDeviceProfile(id);
    }

    @Override
    public void put(DeviceProfile profile) {
        deviceProfiles.put(profile.getId(), profile);
    }

    @Nullable
    @Override
    public DeviceProfile put(ByteString profileBody) {
        Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
        if (deviceProfile.isPresent()) {
            put(deviceProfile.get());
            return deviceProfile.get();
        } else {
            return null;
        }
    }

    @Override
    public void evict(DeviceProfileId id) {
        deviceProfiles.remove(id);
    }


    private DeviceProfile getDeviceProfile(DeviceProfileId id) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            deviceProfileFetchLock.lock();
            try {
                TransportProtos.GetEntityProfileRequestMsg msg = TransportProtos.GetEntityProfileRequestMsg.newBuilder()
                        .setEntityType(EntityType.DEVICE_PROFILE.name())
                        .setEntityIdMSB(id.getId().getMostSignificantBits())
                        .setEntityIdLSB(id.getId().getLeastSignificantBits())
                        .build();
                TransportProtos.GetEntityProfileResponseMsg entityProfileMsg = transportService.getEntityProfile(msg);
                Optional<DeviceProfile> profileOpt = dataDecodingEncodingService.decode(entityProfileMsg.getData().toByteArray());
                if (profileOpt.isPresent()) {
                    profile = profileOpt.get();
                    this.put(profile);
                } else {
                    log.warn("[{}] Can't find device profile: {}", id, entityProfileMsg.getData());
                    throw new RuntimeException("Can't find device profile!");
                }
            } finally {
                deviceProfileFetchLock.unlock();
            }
        }
        return profile;
    }
}
