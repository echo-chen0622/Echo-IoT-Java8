package org.echoiot.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Component
@TbCoreComponent
public class DeviceProfileMsgConstructor {

    @Resource
    private DataDecodingEncodingService dataDecodingEncodingService;

    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        DeviceProfileUpdateMsg.Builder builder = DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits())
                .setName(deviceProfile.getName())
                .setDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile.getProfileData())));
        if (deviceProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        if (deviceProfile.getDescription() != null) {
            builder.setDescription(deviceProfile.getDescription());
        }
        if (deviceProfile.getTransportType() != null) {
            builder.setTransportType(deviceProfile.getTransportType().name());
        }
        if (deviceProfile.getProvisionType() != null) {
            builder.setProvisionType(deviceProfile.getProvisionType().name());
        }
        if (deviceProfile.getProvisionDeviceKey() != null) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
        }
        if (deviceProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(deviceProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (deviceProfile.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(deviceProfile.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(deviceProfile.getFirmwareId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public DeviceProfileUpdateMsg constructDeviceProfileDeleteMsg(DeviceProfileId deviceProfileId) {
        return DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceProfileId.getId().getMostSignificantBits())
                .setIdLSB(deviceProfileId.getId().getLeastSignificantBits()).build();
    }

}
