package org.thingsboard.server.common.data.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by ashvayka on 21.03.18.
 */
@ApiModel
@Data
public class NodeConnectionInfo {
    @ApiModelProperty(position = 1, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    @ApiModelProperty(position = 2, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'to' part of the connection.")
    private int toIndex;
    @ApiModelProperty(position = 3, required = true, value = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}
