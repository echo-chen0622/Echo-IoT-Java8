package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;
import org.jetbrains.annotations.NotNull;

@Data
public class DefaultDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @NotNull
    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
