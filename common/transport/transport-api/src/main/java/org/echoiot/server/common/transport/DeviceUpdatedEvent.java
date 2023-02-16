package org.echoiot.server.common.transport;

import lombok.Getter;
import org.echoiot.server.common.data.Device;

@Getter
public class DeviceUpdatedEvent {
    private final Device device;

    public DeviceUpdatedEvent(Device device) {
        this.device = device;
    }
}
