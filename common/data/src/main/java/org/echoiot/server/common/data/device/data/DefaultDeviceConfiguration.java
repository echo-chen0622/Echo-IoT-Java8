package org.echoiot.server.common.data.device.data;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.echoiot.server.common.data.DeviceProfileType;

@ApiModel
@Data
public class DefaultDeviceConfiguration implements DeviceConfiguration {

    private static final long serialVersionUID = -2225378639573611325L;

    @Override
    public DeviceProfileType getType() {
        return DeviceProfileType.DEFAULT;
    }

}
