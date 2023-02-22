package org.echoiot.server.common.data.device.data;

import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;

@Data
public class DefaultDeviceTransportConfiguration implements DeviceTransportConfiguration {

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
