package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.EdgeVersion;
import org.echoiot.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.echoiot.server.gen.edge.v1.RuleChainUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;

import static org.echoiot.server.service.edge.DefaultEdgeNotificationService.EDGE_IS_ROOT_BODY_KEY;

@Component
@Slf4j
@TbCoreComponent
public class RuleChainEdgeProcessor extends BaseEdgeProcessor {

    @Nullable
    public DownlinkMsg convertRuleChainEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
                RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
                if (ruleChain != null) {
                    boolean isRoot = false;
                    if (edgeEvent.getBody() != null && edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY) != null) {
                        try {
                            isRoot = Boolean.parseBoolean(edgeEvent.getBody().get(EDGE_IS_ROOT_BODY_KEY).asText());
                        } catch (Exception ignored) {}
                    }
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    RuleChainUpdateMsg ruleChainUpdateMsg =
                            ruleChainMsgConstructor.constructRuleChainUpdatedMsg(msgType, ruleChain, isRoot);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addRuleChainUpdateMsg(ruleChainUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainUpdateMsg(ruleChainMsgConstructor.constructRuleChainDeleteMsg(ruleChainId))
                        .build();
                break;
        }
        return downlinkMsg;
    }

    @Nullable
    public DownlinkMsg convertRuleChainMetadataEventToDownlink(@NotNull EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        @NotNull RuleChainId ruleChainId = new RuleChainId(edgeEvent.getEntityId());
        RuleChain ruleChain = ruleChainService.findRuleChainById(edgeEvent.getTenantId(), ruleChainId);
        @Nullable DownlinkMsg downlinkMsg = null;
        if (ruleChain != null) {
            @Nullable RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(edgeEvent.getTenantId(), ruleChainId);
            @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg =
                    ruleChainMsgConstructor.constructRuleChainMetadataUpdatedMsg(edgeEvent.getTenantId(), msgType, ruleChainMetaData, edgeVersion);
            if (ruleChainMetadataUpdateMsg != null) {
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addRuleChainMetadataUpdateMsg(ruleChainMetadataUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processRuleChainNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
