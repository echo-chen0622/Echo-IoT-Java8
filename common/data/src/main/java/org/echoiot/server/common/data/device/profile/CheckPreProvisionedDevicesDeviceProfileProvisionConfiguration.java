package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileProvisionType;
import org.jetbrains.annotations.NotNull;

@Data
public class CheckPreProvisionedDevicesDeviceProfileProvisionConfiguration implements DeviceProfileProvisionConfiguration {

    @NotNull
    private final String provisionDeviceSecret;

    @NotNull
    @Override
    public DeviceProfileProvisionType getType() {
        return DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES;
    }

}
