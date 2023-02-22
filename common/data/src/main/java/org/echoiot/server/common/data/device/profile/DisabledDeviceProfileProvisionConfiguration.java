package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileProvisionType;

@Data
public class DisabledDeviceProfileProvisionConfiguration implements DeviceProfileProvisionConfiguration {

    private final String provisionDeviceSecret;

    @Override
    public DeviceProfileProvisionType getType() {
        return DeviceProfileProvisionType.DISABLED;
    }

}
