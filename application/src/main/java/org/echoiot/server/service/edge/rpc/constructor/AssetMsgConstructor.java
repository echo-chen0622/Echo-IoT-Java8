package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.gen.edge.v1.AssetUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class AssetMsgConstructor {

    @NotNull
    public AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, @NotNull Asset asset) {
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(asset.getUuidId().getMostSignificantBits())
                .setIdLSB(asset.getUuidId().getLeastSignificantBits())
                .setName(asset.getName())
                .setType(asset.getType());
        if (asset.getLabel() != null) {
            builder.setLabel(asset.getLabel());
        }
        if (asset.getCustomerId() != null) {
            builder.setCustomerIdMSB(asset.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(asset.getCustomerId().getId().getLeastSignificantBits());
        }
        if (asset.getAssetProfileId() != null) {
            builder.setAssetProfileIdMSB(asset.getAssetProfileId().getId().getMostSignificantBits());
            builder.setAssetProfileIdLSB(asset.getAssetProfileId().getId().getLeastSignificantBits());
        }
        if (asset.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(asset.getAdditionalInfo()));
        }
        return builder.build();
    }

    @NotNull
    public AssetUpdateMsg constructAssetDeleteMsg(@NotNull AssetId assetId) {
        return AssetUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetId.getId().getMostSignificantBits())
                .setIdLSB(assetId.getId().getLeastSignificantBits()).build();
    }
}
