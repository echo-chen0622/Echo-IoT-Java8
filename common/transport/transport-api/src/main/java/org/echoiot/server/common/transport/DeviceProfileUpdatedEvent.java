package org.echoiot.server.common.transport;

import lombok.Getter;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.queue.discovery.event.TbApplicationEvent;

public final class DeviceProfileUpdatedEvent extends TbApplicationEvent {

    @Getter
    private final DeviceProfile deviceProfile;

    public DeviceProfileUpdatedEvent(DeviceProfile deviceProfile) {
        super(new Object());
        this.deviceProfile = deviceProfile;
    }
}
