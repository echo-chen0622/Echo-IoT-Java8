package org.echoiot.server.service.edge.rpc.constructor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.gen.edge.v1.*;
import org.echoiot.server.queue.util.DataDecodingEncodingService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
@TbCoreComponent
public class DeviceMsgConstructor {

    @Resource
    private DataDecodingEncodingService dataDecodingEncodingService;

    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device, @Nullable String conflictName) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        if (device.getCustomerId() != null) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }
        if (device.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits());
            builder.setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());
        }
        if (device.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));
        }
        if (device.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(device.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(device.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (conflictName != null) {
            builder.setConflictName(conflictName);
        }
        if (device.getDeviceData() != null) {
            builder.setDeviceDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(device.getDeviceData())));
        }
        return builder.build();
    }

    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        DeviceCredentialsUpdateMsg.Builder builder = DeviceCredentialsUpdateMsg.newBuilder()
                                                                                        .setDeviceIdMSB(deviceCredentials.getDeviceId().getId().getMostSignificantBits())
                                                                                        .setDeviceIdLSB(deviceCredentials.getDeviceId().getId().getLeastSignificantBits());
        if (deviceCredentials.getCredentialsType() != null) {
            builder.setCredentialsType(deviceCredentials.getCredentialsType().name())
                    .setCredentialsId(deviceCredentials.getCredentialsId());
        }
        if (deviceCredentials.getCredentialsValue() != null) {
            builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
        }
        return builder.build();
    }

    public DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId) {
        return DeviceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceId.getId().getMostSignificantBits())
                .setIdLSB(deviceId.getId().getLeastSignificantBits()).build();
    }

    public DeviceRpcCallMsg constructDeviceRpcCallMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = constructDeviceRpcMsg(deviceId, body);
        if (body.has("error") || body.has("response")) {
            RpcResponseMsg.Builder responseBuilder = RpcResponseMsg.newBuilder();
            if (body.has("error")) {
                responseBuilder.setError(body.get("error").asText());
            } else {
                responseBuilder.setResponse(body.get("response").asText());
            }
            builder.setResponseMsg(responseBuilder.build());
        } else {
            RpcRequestMsg.Builder requestBuilder = RpcRequestMsg.newBuilder();
            requestBuilder.setMethod(body.get("method").asText());
            requestBuilder.setParams(body.get("params").asText());
            builder.setRequestMsg(requestBuilder.build());
        }
        return builder.build();
    }

    private DeviceRpcCallMsg.Builder constructDeviceRpcMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = DeviceRpcCallMsg.newBuilder()
                                                                    .setDeviceIdMSB(deviceId.getMostSignificantBits())
                                                                    .setDeviceIdLSB(deviceId.getLeastSignificantBits())
                                                                    .setRequestId(body.get("requestId").asInt());
        if (body.get("oneway") != null) {
            builder.setOneway(body.get("oneway").asBoolean());
        }
        if (body.get("requestUUID") != null) {
            UUID requestUUID = UUID.fromString(body.get("requestUUID").asText());
            builder.setRequestUuidMSB(requestUUID.getMostSignificantBits())
                    .setRequestUuidLSB(requestUUID.getLeastSignificantBits());
        }
        if (body.get("expirationTime") != null) {
            builder.setExpirationTime(body.get("expirationTime").asLong());
        }
        if (body.get("persisted") != null) {
            builder.setPersisted(body.get("persisted").asBoolean());
        }
        if (body.get("retries") != null) {
            builder.setRetries(body.get("retries").asInt());
        }
        if (body.get("additionalInfo") != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(body.get("additionalInfo")));
        }
        if (body.get("serviceId") != null) {
            builder.setServiceId(body.get("serviceId").asText());
        }
        if (body.get("sessionId") != null) {
            builder.setSessionId(body.get("sessionId").asText());
        }
        return builder;
    }
}
