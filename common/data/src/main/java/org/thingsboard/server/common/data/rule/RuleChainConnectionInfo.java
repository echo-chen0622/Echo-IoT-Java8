package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;

/**
 * Created by ashvayka on 21.03.18.
 */
@ApiModel
@Data
public class RuleChainConnectionInfo {
    @ApiModelProperty(position = 1, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    @ApiModelProperty(position = 2, required = true, value = "JSON object with the Rule Chain Id.")
    private RuleChainId targetRuleChainId;
    @ApiModelProperty(position = 3, required = true, value = "JSON object with the additional information about the connection.")
    private JsonNode additionalInfo;
    @ApiModelProperty(position = 4, required = true, value = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}
