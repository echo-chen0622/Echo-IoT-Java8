package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@ApiModel
@Data
public class AlarmConditionFilterKey implements Serializable {

    @ApiModelProperty(position = 1, value = "The key type", example = "TIME_SERIES")
    private final AlarmConditionKeyType type;
    @NoXss
    @ApiModelProperty(position = 2, value = "String value representing the key", example = "temp")
    private final String key;

}
