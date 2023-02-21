package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class SaveDeviceWithCredentialsRequest {

    @NotNull
    @ApiModelProperty(position = 1, value = "The JSON with device entity.", required = true)
    private final Device device;
    @NotNull
    @ApiModelProperty(position = 2, value = "The JSON with credentials entity.", required = true)
    private final DeviceCredentials credentials;

}
