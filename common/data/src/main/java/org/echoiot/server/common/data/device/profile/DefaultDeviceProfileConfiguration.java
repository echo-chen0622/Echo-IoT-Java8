package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileType;

@Data
public class DefaultDeviceProfileConfiguration implements DeviceProfileConfiguration {

    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
