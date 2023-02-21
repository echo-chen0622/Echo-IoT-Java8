package org.echoiot.server.common.data.device.data;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileType;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class DefaultDeviceConfiguration implements DeviceConfiguration {

    private static final long serialVersionUID = -2225378639573611325L;

    @NotNull
    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
