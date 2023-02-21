package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.gen.edge.v1.AssetUpdateMsg;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@TbCoreComponent
public class AssetEdgeProcessor extends BaseEdgeProcessor {

    @Nullable
    public DownlinkMsg convertAssetEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull AssetId assetId = new AssetId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Asset asset = assetService.findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg =
                            assetMsgConstructor.constructAssetUpdatedMsg(msgType, asset);
                    @NotNull DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                                                                      .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                                                                      .addAssetUpdateMsg(assetUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        builder.addAssetProfileUpdateMsg(assetProfileMsgConstructor.constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg =
                        assetMsgConstructor.constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processAssetNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
