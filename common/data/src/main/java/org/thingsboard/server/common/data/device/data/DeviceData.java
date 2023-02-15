package org.thingsboard.server.common.data.device.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class DeviceData implements Serializable {

    private static final long serialVersionUID = -3771567735290681274L;

    @ApiModelProperty(position = 1, value = "Device configuration for device profile type. DEFAULT is only supported value for now")
    private DeviceConfiguration configuration;
    @ApiModelProperty(position = 2, value = "Device transport configuration used to connect the device")
    private DeviceTransportConfiguration transportConfiguration;

}
