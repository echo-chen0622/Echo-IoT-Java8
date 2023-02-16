package org.echoiot.server.common.transport.session;

import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Optional;
import java.util.UUID;

public interface SessionContext {

    UUID getSessionId();

    int nextMsgId();

    void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile);

    void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt);
}
