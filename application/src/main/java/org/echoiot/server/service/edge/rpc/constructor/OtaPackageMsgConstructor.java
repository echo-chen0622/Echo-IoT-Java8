package org.echoiot.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class OtaPackageMsgConstructor {

    @NotNull
    public OtaPackageUpdateMsg constructOtaPackageUpdatedMsg(UpdateMsgType msgType, @NotNull OtaPackage otaPackage) {
        OtaPackageUpdateMsg.Builder builder = OtaPackageUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(otaPackage.getId().getId().getMostSignificantBits())
                .setIdLSB(otaPackage.getId().getId().getLeastSignificantBits())
                .setType(otaPackage.getType().name())
                .setTitle(otaPackage.getTitle())
                .setVersion(otaPackage.getVersion())
                .setTag(otaPackage.getTag());

        if (otaPackage.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(otaPackage.getDeviceProfileId().getId().getMostSignificantBits())
                    .setDeviceProfileIdLSB(otaPackage.getDeviceProfileId().getId().getLeastSignificantBits());
        }

        if (otaPackage.getUrl() != null) {
            builder.setUrl(otaPackage.getUrl());
        }
        if (otaPackage.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(otaPackage.getAdditionalInfo()));
        }
        if (otaPackage.getFileName() != null) {
            builder.setFileName(otaPackage.getFileName());
        }
        if (otaPackage.getContentType() != null) {
            builder.setContentType(otaPackage.getContentType());
        }
        if (otaPackage.getChecksumAlgorithm() != null) {
            builder.setChecksumAlgorithm(otaPackage.getChecksumAlgorithm().name());
        }
        if (otaPackage.getChecksum() != null) {
            builder.setChecksum(otaPackage.getChecksum());
        }
        if (otaPackage.getDataSize() != null) {
            builder.setDataSize(otaPackage.getDataSize());
        }
        if (otaPackage.getData() != null) {
            builder.setData(ByteString.copyFrom(otaPackage.getData().array()));
        }
        return builder.build();
    }

    @NotNull
    public OtaPackageUpdateMsg constructOtaPackageDeleteMsg(@NotNull OtaPackageId otaPackageId) {
        return OtaPackageUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(otaPackageId.getId().getMostSignificantBits())
                .setIdLSB(otaPackageId.getId().getLeastSignificantBits()).build();
    }

}
