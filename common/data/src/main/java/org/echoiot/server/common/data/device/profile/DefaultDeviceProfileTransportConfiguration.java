package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceTransportType;

@Data
public class DefaultDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
