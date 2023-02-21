package org.echoiot.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
public class AssetProfileMsgConstructor {

    @NotNull
    public AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, @NotNull AssetProfile assetProfile) {
        @NotNull AssetProfileUpdateMsg.Builder builder = AssetProfileUpdateMsg.newBuilder()
                                                                              .setMsgType(msgType)
                                                                              .setIdMSB(assetProfile.getId().getId().getMostSignificantBits())
                                                                              .setIdLSB(assetProfile.getId().getId().getLeastSignificantBits())
                                                                              .setName(assetProfile.getName())
                                                                              .setDefault(assetProfile.isDefault());
        if (assetProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(assetProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(assetProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        if (assetProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(assetProfile.getDefaultQueueName());
        }
        if (assetProfile.getDescription() != null) {
            builder.setDescription(assetProfile.getDescription());
        }
        if (assetProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(assetProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        return builder.build();
    }

    @NotNull
    public AssetProfileUpdateMsg constructAssetProfileDeleteMsg(@NotNull AssetProfileId assetProfileId) {
        return AssetProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetProfileId.getId().getMostSignificantBits())
                .setIdLSB(assetProfileId.getId().getLeastSignificantBits()).build();
    }

}
