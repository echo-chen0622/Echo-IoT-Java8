package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class LwM2mInstance {
    @ApiModelProperty(position = 1, value = "LwM2M Instance id.", example = "0")
    int id;
    @ApiModelProperty(position = 2, value = "LwM2M Resource observe.")
    LwM2mResourceObserve[] resources;

}
