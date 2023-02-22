package org.echoiot.server.common.data.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.id.RuleChainId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
@ApiModel
@Data
public class RuleChainMetaData {

    @ApiModelProperty(position = 1, required = true, value = "JSON object with Rule Chain Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;

    @ApiModelProperty(position = 2, required = true, value = "Index of the first rule node in the 'nodes' list")
    private Integer firstNodeIndex;

    @ApiModelProperty(position = 3, required = true, value = "List of rule node JSON objects")
    private List<RuleNode> nodes;

    @ApiModelProperty(position = 4, required = true, value = "List of JSON objects that represent connections between rule nodes")
    private List<NodeConnectionInfo> connections;

    @ApiModelProperty(position = 5, required = true, value = "List of JSON objects that represent connections between rule nodes and other rule chains.")
    private List<RuleChainConnectionInfo> ruleChainConnections;

    public void addConnectionInfo(int fromIndex, int toIndex, String type) {
        NodeConnectionInfo connectionInfo = new NodeConnectionInfo();
        connectionInfo.setFromIndex(fromIndex);
        connectionInfo.setToIndex(toIndex);
        connectionInfo.setType(type);
        if (connections == null) {
            connections = new ArrayList<>();
        }
        connections.add(connectionInfo);
    }
}
