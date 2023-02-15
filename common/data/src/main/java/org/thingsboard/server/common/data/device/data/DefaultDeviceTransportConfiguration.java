package org.thingsboard.server.common.data.device.data;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;

@Data
public class DefaultDeviceTransportConfiguration implements DeviceTransportConfiguration {

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
