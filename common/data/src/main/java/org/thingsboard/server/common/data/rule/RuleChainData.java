package org.thingsboard.server.common.data.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@ApiModel
@Data
public class RuleChainData {

    @ApiModelProperty(position = 1, required = true, value = "List of the Rule Chain objects.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    List<RuleChain> ruleChains;
    @ApiModelProperty(position = 2, required = true, value = "List of the Rule Chain metadata objects.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    List<RuleChainMetaData> metadata;
}
