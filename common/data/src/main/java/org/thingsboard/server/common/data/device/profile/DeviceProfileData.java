package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

@ApiModel
@Data
public class DeviceProfileData implements Serializable {

    @ApiModelProperty(position = 1, value = "JSON object of device profile configuration")
    private DeviceProfileConfiguration configuration;
    @Valid
    @ApiModelProperty(position = 2, value = "JSON object of device profile transport configuration")
    private DeviceProfileTransportConfiguration transportConfiguration;
    @ApiModelProperty(position = 3, value = "JSON object of provisioning strategy type per device profile")
    private DeviceProfileProvisionConfiguration provisionConfiguration;
    @Valid
    @ApiModelProperty(position = 4, value = "JSON array of alarm rules configuration per device profile")
    private List<DeviceProfileAlarm> alarms;

}
