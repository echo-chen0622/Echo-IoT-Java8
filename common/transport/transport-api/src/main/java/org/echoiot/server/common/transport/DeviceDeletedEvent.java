package org.echoiot.server.common.transport;

import lombok.Getter;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.queue.discovery.event.TbApplicationEvent;

public final class DeviceDeletedEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -7453664970966733857L;
    @Getter
    private final DeviceId deviceId;

    public DeviceDeletedEvent(DeviceId deviceId) {
        super(new Object());
        this.deviceId = deviceId;
    }
}
