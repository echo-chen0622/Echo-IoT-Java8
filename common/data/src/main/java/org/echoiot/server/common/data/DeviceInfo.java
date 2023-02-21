package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class DeviceInfo extends Device {

    @ApiModelProperty(position = 13, value = "Title of the Customer that owns the device.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String customerTitle;
    @ApiModelProperty(position = 14, value = "Indicates special 'Public' Customer that is auto-generated to use the devices on public dashboards.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean customerIsPublic;
    @ApiModelProperty(position = 15, value = "Name of the corresponding Device Profile.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String deviceProfileName;

    public DeviceInfo() {
        super();
    }

    public DeviceInfo(DeviceId deviceId) {
        super(deviceId);
    }

    public DeviceInfo(@NotNull Device device, String customerTitle, boolean customerIsPublic, String deviceProfileName) {
        super(device);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
        this.deviceProfileName = deviceProfileName;
    }
}
