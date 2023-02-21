package org.echoiot.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@ApiModel
@Data
public class AlarmConditionFilterKey implements Serializable {

    @NotNull
    @ApiModelProperty(position = 1, value = "The key type", example = "TIME_SERIES")
    private final AlarmConditionKeyType type;
    @NotNull
    @NoXss
    @ApiModelProperty(position = 2, value = "String value representing the key", example = "temp")
    private final String key;

}
