package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2mObject {
    @ApiModelProperty(position = 1, value = "LwM2M Object id.", example = "19")
    int id;
    @ApiModelProperty(position = 2, value = "LwM2M Object key id.", example = "19_1.0")
    String keyId;
    @ApiModelProperty(position = 3, value = "LwM2M Object name.", example = "BinaryAppDataContainer")
    String name;
    @ApiModelProperty(position = 4, value = "LwM2M Object multiple.", example = "true")
    boolean multiple;
    @ApiModelProperty(position = 5, value = "LwM2M Object mandatory.", example = "false")
    boolean mandatory;
    @ApiModelProperty(position = 6, value = "LwM2M Object instances.")
    LwM2mInstance [] instances;
}
