package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;

@Data
public class DefaultDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.DEFAULT;
    }

}
