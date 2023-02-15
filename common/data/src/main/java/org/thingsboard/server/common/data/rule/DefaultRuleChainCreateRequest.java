package org.thingsboard.server.common.data.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@ApiModel
@Data
@Slf4j
public class DefaultRuleChainCreateRequest implements Serializable {

    private static final long serialVersionUID = 5600333716030561537L;

    @ApiModelProperty(position = 1, required = true, value = "Name of the new rule chain", example = "Root Rule Chain")
    private String name;

}
