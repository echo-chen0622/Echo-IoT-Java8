package org.echoiot.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.NodeConnectionInfo;
import org.echoiot.server.common.data.rule.RuleChainConnectionInfo;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.flow.TbRuleChainInputNode;
import org.echoiot.rule.engine.flow.TbRuleChainInputNodeConfiguration;
import org.echoiot.rule.engine.flow.TbRuleChainOutputNode;
import org.echoiot.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class RuleChainMetadataConstructorV330 extends AbstractRuleChainMetadataConstructor {

    private static final String RULE_CHAIN_INPUT_NODE = TbRuleChainInputNode.class.getName();
    private static final String TB_RULE_CHAIN_OUTPUT_NODE = TbRuleChainOutputNode.class.getName();

    @Override
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        @NotNull RuleChainMetadataUpdateMsg.Builder builder,
                                                        @NotNull RuleChainMetaData ruleChainMetaData) throws JsonProcessingException {
        @NotNull List<RuleNode> supportedNodes = filterNodes(ruleChainMetaData.getNodes());

        @NotNull NavigableSet<Integer> removedNodeIndexes = getRemovedNodeIndexes(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections());
        @NotNull List<NodeConnectionInfo> connections = filterConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections(), removedNodeIndexes);

        @NotNull List<RuleChainConnectionInfo> ruleChainConnections = new ArrayList<>();
        if (ruleChainMetaData.getRuleChainConnections() != null) {
            ruleChainConnections.addAll(ruleChainMetaData.getRuleChainConnections());
        }
        ruleChainConnections.addAll(addRuleChainConnections(ruleChainMetaData.getNodes(), ruleChainMetaData.getConnections()));
        builder.addAllNodes(constructNodes(supportedNodes))
                .addAllConnections(constructConnections(connections))
                .addAllRuleChainConnections(constructRuleChainConnections(ruleChainConnections, removedNodeIndexes));
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            @NotNull Integer firstNodeIndex = ruleChainMetaData.getFirstNodeIndex();
            // decrease index because of removed nodes
            for (Integer removedIndex : removedNodeIndexes) {
                if (firstNodeIndex > removedIndex) {
                    firstNodeIndex = firstNodeIndex - 1;
                }
            }
            builder.setFirstNodeIndex(firstNodeIndex);
        } else {
            builder.setFirstNodeIndex(-1);
        }
    }

    @NotNull
    private NavigableSet<Integer> getRemovedNodeIndexes(@NotNull List<RuleNode> nodes, @NotNull List<NodeConnectionInfo> connections) {
        @NotNull TreeSet<Integer> removedIndexes = new TreeSet<>();
        for (@NotNull NodeConnectionInfo connection : connections) {
            for (int i = 0; i < nodes.size(); i++) {
                RuleNode node = nodes.get(i);
                if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                        || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                    if (connection.getFromIndex() == i || connection.getToIndex() == i) {
                        removedIndexes.add(i);
                    }
                }
            }
        }
        return removedIndexes.descendingSet();
    }

    @NotNull
    private List<NodeConnectionInfo> filterConnections(@NotNull List<RuleNode> nodes,
                                                       @Nullable List<NodeConnectionInfo> connections,
                                                       @NotNull NavigableSet<Integer> removedNodeIndexes) {
        @NotNull List<NodeConnectionInfo> result = new ArrayList<>();
        if (connections != null) {
            result = connections.stream().filter(conn -> {
                for (int i = 0; i < nodes.size(); i++) {
                    RuleNode node = nodes.get(i);
                    if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)
                            || node.getType().equalsIgnoreCase(TB_RULE_CHAIN_OUTPUT_NODE)) {
                        if (conn.getFromIndex() == i || conn.getToIndex() == i) {
                            return false;
                        }
                    }
                }
                return true;
            }).map(conn -> {
                @NotNull NodeConnectionInfo newConn = new NodeConnectionInfo();
                newConn.setFromIndex(conn.getFromIndex());
                newConn.setToIndex(conn.getToIndex());
                newConn.setType(conn.getType());
                return newConn;
            }).collect(Collectors.toList());
        }

        // decrease index because of removed nodes
        for (Integer removedIndex : removedNodeIndexes) {
            for (@NotNull NodeConnectionInfo newConn : result) {
                if (newConn.getToIndex() > removedIndex) {
                    newConn.setToIndex(newConn.getToIndex() - 1);
                }
                if (newConn.getFromIndex() > removedIndex) {
                    newConn.setFromIndex(newConn.getFromIndex() - 1);
                }
            }
        }

        return result;
    }

    @NotNull
    private List<RuleNode> filterNodes(@NotNull List<RuleNode> nodes) {
        @NotNull List<RuleNode> result = new ArrayList<>();
        for (@NotNull RuleNode node : nodes) {
            if (RULE_CHAIN_INPUT_NODE.equals(node.getType())
                    || TB_RULE_CHAIN_OUTPUT_NODE.equals(node.getType())) {
                log.trace("Skipping not supported rule node {}", node);
            } else {
                result.add(node);
            }
        }
        return result;
    }

    @NotNull
    private List<RuleChainConnectionInfo> addRuleChainConnections(@NotNull List<RuleNode> nodes, @NotNull List<NodeConnectionInfo> connections) {
        @NotNull List<RuleChainConnectionInfo> result = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            RuleNode node = nodes.get(i);
            if (node.getType().equalsIgnoreCase(RULE_CHAIN_INPUT_NODE)) {
                for (@NotNull NodeConnectionInfo connection : connections) {
                    if (connection.getToIndex() == i) {
                        @NotNull RuleChainConnectionInfo e = new RuleChainConnectionInfo();
                        e.setFromIndex(connection.getFromIndex());
                        TbRuleChainInputNodeConfiguration configuration = JacksonUtil.treeToValue(node.getConfiguration(), TbRuleChainInputNodeConfiguration.class);
                        e.setTargetRuleChainId(new RuleChainId(UUID.fromString(configuration.getRuleChainId())));
                        e.setAdditionalInfo(node.getAdditionalInfo());
                        e.setType(connection.getType());
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }
}
