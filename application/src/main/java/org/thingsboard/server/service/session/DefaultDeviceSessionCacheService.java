package org.thingsboard.server.service.session;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.Serializable;
import java.util.Collections;

import static org.thingsboard.server.common.data.CacheConstants.SESSIONS_CACHE;

/**
 * Created by ashvayka on 29.10.18.
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultDeviceSessionCacheService implements DeviceSessionCacheService {

    @Autowired
    protected TbTransactionalCache<DeviceId, DeviceSessionsCacheEntry> cache;

    @Override
    public DeviceSessionsCacheEntry get(DeviceId deviceId) {
        log.debug("[{}] Fetching session data from cache", deviceId);
        return cache.getAndPutInTransaction(deviceId, () ->
                DeviceSessionsCacheEntry.newBuilder().addAllSessions(Collections.emptyList()).build(), false);
    }

    @Override
    public DeviceSessionsCacheEntry put(DeviceId deviceId, DeviceSessionsCacheEntry sessions) {
        log.debug("[{}] Pushing session data to cache: {}", deviceId, sessions);
        cache.put(deviceId, sessions);
        return sessions;
    }
}
