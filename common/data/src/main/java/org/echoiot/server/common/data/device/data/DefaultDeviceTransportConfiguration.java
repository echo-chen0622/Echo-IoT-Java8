package org.echoiot.server.common.data.device.data;

import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;
import org.jetbrains.annotations.NotNull;

@Data
public class DefaultDeviceTransportConfiguration implements DeviceTransportConfiguration {

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
