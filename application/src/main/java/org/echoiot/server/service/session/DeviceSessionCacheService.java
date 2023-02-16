package org.echoiot.server.service.session;

import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;

/**
 * Created by Echo on 29.10.18.
 */
public interface DeviceSessionCacheService {

    DeviceSessionsCacheEntry get(DeviceId deviceId);

    DeviceSessionsCacheEntry put(DeviceId deviceId, DeviceSessionsCacheEntry sessions);

}
