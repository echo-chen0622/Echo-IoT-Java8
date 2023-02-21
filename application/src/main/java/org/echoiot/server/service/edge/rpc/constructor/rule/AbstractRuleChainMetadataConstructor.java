package org.echoiot.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.NodeConnectionInfo;
import org.echoiot.server.common.data.rule.RuleChainConnectionInfo;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.gen.edge.v1.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

@Slf4j
@AllArgsConstructor
public abstract class AbstractRuleChainMetadataConstructor implements RuleChainMetadataConstructor {

    @Nullable
    @Override
    public RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                           UpdateMsgType msgType,
                                                                           @NotNull RuleChainMetaData ruleChainMetaData) {
        try {
            RuleChainMetadataUpdateMsg.Builder builder = RuleChainMetadataUpdateMsg.newBuilder();
            builder.setRuleChainIdMSB(ruleChainMetaData.getRuleChainId().getId().getMostSignificantBits())
                    .setRuleChainIdLSB(ruleChainMetaData.getRuleChainId().getId().getLeastSignificantBits());
            constructRuleChainMetadataUpdatedMsg(tenantId, builder, ruleChainMetaData);
            builder.setMsgType(msgType);
            return builder.build();
        } catch (JsonProcessingException ex) {
            log.error("Can't construct RuleChainMetadataUpdateMsg", ex);
        }
        return null;
    }

    protected abstract void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                 RuleChainMetadataUpdateMsg.Builder builder,
                                                                 RuleChainMetaData ruleChainMetaData) throws JsonProcessingException;

    @NotNull
    protected List<NodeConnectionInfoProto> constructConnections(@Nullable List<NodeConnectionInfo> connections) {
        @NotNull List<NodeConnectionInfoProto> result = new ArrayList<>();
        if (connections != null && !connections.isEmpty()) {
            for (@NotNull NodeConnectionInfo connection : connections) {
                result.add(constructConnection(connection));
            }
        }
        return result;
    }

    @NotNull
    private NodeConnectionInfoProto constructConnection(@NotNull NodeConnectionInfo connection) {
        return NodeConnectionInfoProto.newBuilder()
                .setFromIndex(connection.getFromIndex())
                .setToIndex(connection.getToIndex())
                .setType(connection.getType())
                .build();
    }

    @NotNull
    protected List<RuleNodeProto> constructNodes(@Nullable List<RuleNode> nodes) throws JsonProcessingException {
        @NotNull List<RuleNodeProto> result = new ArrayList<>();
        if (nodes != null && !nodes.isEmpty()) {
            for (@NotNull RuleNode node : nodes) {
                result.add(constructNode(node));
            }
        }
        return result;
    }

    @NotNull
    private RuleNodeProto constructNode(@NotNull RuleNode node) throws JsonProcessingException {
        return RuleNodeProto.newBuilder()
                .setIdMSB(node.getId().getId().getMostSignificantBits())
                .setIdLSB(node.getId().getId().getLeastSignificantBits())
                .setType(node.getType())
                .setName(node.getName())
                .setDebugMode(node.isDebugMode())
                .setConfiguration(JacksonUtil.OBJECT_MAPPER.writeValueAsString(node.getConfiguration()))
                .setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.writeValueAsString(node.getAdditionalInfo()))
                .build();
    }

    @NotNull
    protected List<RuleChainConnectionInfoProto> constructRuleChainConnections(@Nullable List<RuleChainConnectionInfo> ruleChainConnections,
                                                                               @NotNull NavigableSet<Integer> removedNodeIndexes) throws JsonProcessingException {
        @NotNull List<RuleChainConnectionInfoProto> result = new ArrayList<>();
        if (ruleChainConnections != null && !ruleChainConnections.isEmpty()) {
            for (@NotNull RuleChainConnectionInfo ruleChainConnectionInfo : ruleChainConnections) {
                if (!removedNodeIndexes.isEmpty()) { // 3_3_0 only
                    int fromIndex = ruleChainConnectionInfo.getFromIndex();
                    // decrease index because of removed nodes
                    for (Integer removedIndex : removedNodeIndexes) {
                        if (fromIndex > removedIndex) {
                            fromIndex = fromIndex - 1;
                        }
                    }
                    ruleChainConnectionInfo.setFromIndex(fromIndex);
                    ObjectNode additionalInfo = (ObjectNode) ruleChainConnectionInfo.getAdditionalInfo();
                    if (additionalInfo.get("ruleChainNodeId") == null) {
                        additionalInfo.put("ruleChainNodeId", "rule-chain-node-UNDEFINED");
                    }
                }
                result.add(constructRuleChainConnection(ruleChainConnectionInfo));
            }
        }
        return result;
    }

    @NotNull
    private RuleChainConnectionInfoProto constructRuleChainConnection(@NotNull RuleChainConnectionInfo ruleChainConnectionInfo) throws JsonProcessingException {
        return RuleChainConnectionInfoProto.newBuilder()
                .setFromIndex(ruleChainConnectionInfo.getFromIndex())
                .setTargetRuleChainIdMSB(ruleChainConnectionInfo.getTargetRuleChainId().getId().getMostSignificantBits())
                .setTargetRuleChainIdLSB(ruleChainConnectionInfo.getTargetRuleChainId().getId().getLeastSignificantBits())
                .setType(ruleChainConnectionInfo.getType())
                .setAdditionalInfo(JacksonUtil.OBJECT_MAPPER.writeValueAsString(ruleChainConnectionInfo.getAdditionalInfo()))
                .build();
    }
}
