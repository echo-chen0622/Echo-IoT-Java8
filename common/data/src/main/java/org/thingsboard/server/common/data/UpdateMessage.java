package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class UpdateMessage {

    @ApiModelProperty(position = 1, value = "The message about new platform update available.")
    private final String message;
    @ApiModelProperty(position = 1, value = "'True' if new platform update is available.")
    private final boolean isUpdateAvailable;

}
