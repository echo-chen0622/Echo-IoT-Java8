package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import javax.validation.Valid;
import java.util.List;

@ApiModel
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmCondition implements Serializable {

    @Valid
    @ApiModelProperty(position = 1, value = "JSON array of alarm condition filters")
    private List<AlarmConditionFilter> condition;
    @ApiModelProperty(position = 2, value = "JSON object representing alarm condition type")
    private AlarmConditionSpec spec;

}
