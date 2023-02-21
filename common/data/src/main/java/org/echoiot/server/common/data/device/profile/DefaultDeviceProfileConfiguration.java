package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileType;
import org.jetbrains.annotations.NotNull;

@Data
public class DefaultDeviceProfileConfiguration implements DeviceProfileConfiguration {

    @NotNull
    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
