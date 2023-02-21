package org.echoiot.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.edge.rpc.constructor.rule.RuleChainMetadataConstructor;
import org.echoiot.server.service.edge.rpc.constructor.rule.RuleChainMetadataConstructorFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.gen.edge.v1.EdgeVersion;
import org.echoiot.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.echoiot.server.gen.edge.v1.RuleChainUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;

@Component
@Slf4j
@TbCoreComponent
public class RuleChainMsgConstructor {

    @NotNull
    public RuleChainUpdateMsg constructRuleChainUpdatedMsg(UpdateMsgType msgType, @NotNull RuleChain ruleChain, boolean isRoot) {
        RuleChainUpdateMsg.Builder builder = RuleChainUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(ruleChain.getId().getId().getMostSignificantBits())
                .setIdLSB(ruleChain.getId().getId().getLeastSignificantBits())
                .setName(ruleChain.getName())
                .setRoot(isRoot)
                .setDebugMode(ruleChain.isDebugMode())
                .setConfiguration(JacksonUtil.toString(ruleChain.getConfiguration()));
        if (ruleChain.getFirstRuleNodeId() != null) {
            builder.setFirstRuleNodeIdMSB(ruleChain.getFirstRuleNodeId().getId().getMostSignificantBits())
                    .setFirstRuleNodeIdLSB(ruleChain.getFirstRuleNodeId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                           UpdateMsgType msgType,
                                                                           RuleChainMetaData ruleChainMetaData,
                                                                           @NotNull EdgeVersion edgeVersion) {
        @NotNull RuleChainMetadataConstructor ruleChainMetadataConstructor
                = RuleChainMetadataConstructorFactory.getByEdgeVersion(edgeVersion);
        return ruleChainMetadataConstructor.constructRuleChainMetadataUpdatedMsg(tenantId, msgType, ruleChainMetaData);
    }

    @NotNull
    public RuleChainUpdateMsg constructRuleChainDeleteMsg(@NotNull RuleChainId ruleChainId) {
        return RuleChainUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setIdLSB(ruleChainId.getId().getLeastSignificantBits()).build();
    }
}
