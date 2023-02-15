package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.Device;

@Getter
public class DeviceUpdatedEvent {
    private final Device device;

    public DeviceUpdatedEvent(Device device) {
        this.device = device;
    }
}
