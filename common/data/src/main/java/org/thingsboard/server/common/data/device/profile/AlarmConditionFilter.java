package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;

import java.io.Serializable;

@ApiModel
@Data
public class AlarmConditionFilter implements Serializable {

    @Valid
    @ApiModelProperty(position = 1, value = "JSON object for specifying alarm condition by specific key")
    private AlarmConditionFilterKey key;
    @ApiModelProperty(position = 2, value = "String representation of the type of the value", example = "NUMERIC")
    private EntityKeyValueType valueType;
    @NoXss
    @ApiModelProperty(position = 3, value = "Value used in Constant comparison. For other types, such as TIME_SERIES or ATTRIBUTE, the predicate condition is used")
    private Object value;
    @Valid
    @ApiModelProperty(position = 4, value = "JSON object representing filter condition")
    private KeyFilterPredicate predicate;

}
