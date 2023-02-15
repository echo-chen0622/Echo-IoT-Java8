package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@ApiModel
@Data
public class SaveDeviceWithCredentialsRequest {

    @ApiModelProperty(position = 1, value = "The JSON with device entity.", required = true)
    private final Device device;
    @ApiModelProperty(position = 2, value = "The JSON with credentials entity.", required = true)
    private final DeviceCredentials credentials;

}
