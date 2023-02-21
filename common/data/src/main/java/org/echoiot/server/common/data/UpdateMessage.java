package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class UpdateMessage {

    @NotNull
    @ApiModelProperty(position = 1, value = "The message about new platform update available.")
    private final String message;
    @ApiModelProperty(position = 1, value = "'True' if new platform update is available.")
    private final boolean isUpdateAvailable;

}
