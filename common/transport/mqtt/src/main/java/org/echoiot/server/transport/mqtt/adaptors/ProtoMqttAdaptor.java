package org.echoiot.server.transport.mqtt.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.profile.MqttTopics;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.common.transport.adaptor.ProtoConverter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.server.gen.transport.TransportApiProtos;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.mqtt.session.DeviceSessionCtx;
import org.echoiot.server.transport.mqtt.session.MqttDeviceAwareSessionContext;

import java.util.Optional;

@Component
@Slf4j
public class ProtoMqttAdaptor implements MqttTransportAdaptor {

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        @NotNull byte[] bytes = toBytes(inbound.payload());
        @NotNull Descriptors.Descriptor telemetryDynamicMsgDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getTelemetryDynamicMsgDescriptor());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, telemetryDynamicMsgDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        @NotNull byte[] bytes = toBytes(inbound.payload());
        @NotNull Descriptors.Descriptor attributesDynamicMessageDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getAttributesDynamicMessageDescriptor());
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, attributesDynamicMessageDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post attributes request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        @NotNull byte[] bytes = toBytes(inbound.payload());
        try {
            return ProtoConverter.convertToClaimDeviceProto(ctx.getDeviceId(), bytes);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode claim device request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        @NotNull byte[] bytes = toBytes(inbound.payload());
        String topicName = inbound.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topicBase);
            return ProtoConverter.convertToGetAttributeRequestMessage(bytes, requestId);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage mqttMsg, @NotNull String topicBase) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        String topicName = mqttMsg.variableHeader().topicName();
        @NotNull byte[] bytes = toBytes(mqttMsg.payload());
        @NotNull Descriptors.Descriptor rpcResponseDynamicMessageDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getRpcResponseDynamicMessageDescriptor());
        try {
            int requestId = getRequestId(topicName, topicBase);
            JsonElement response = new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, rpcResponseDynamicMessageDescriptor));
            return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(response.toString()).build();
        } catch (Exception e) {
            log.debug("Failed to decode rpc response", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage mqttMsg, @NotNull String topicBase) throws AdaptorException {
        @NotNull byte[] bytes = toBytes(mqttMsg.payload());
        String topicName = mqttMsg.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topicBase);
            return ProtoConverter.convertToServerRpcRequest(bytes, requestId);
        } catch (InvalidProtocolBufferException e) {
            log.debug("Failed to decode to server rpc request", e);
            throw new AdaptorException(e);
        }
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage mqttMsg) throws AdaptorException {
        @NotNull byte[] bytes = toBytes(mqttMsg.payload());
        try {
            return ProtoConverter.convertToProvisionRequestMsg(bytes);
        } catch (InvalidProtocolBufferException ex) {
            log.debug("Failed to decode provision request", ex);
            throw new AdaptorException(ex);
        }
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            int requestId = responseMsg.getRequestId();
            if (requestId >= 0) {
                return Optional.of(createMqttPublishMsg(ctx, topicBase + requestId, responseMsg.toByteArray()));
            }
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.ToDeviceRpcRequestMsg rpcRequest, String topicBase) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        DynamicMessage.Builder rpcRequestDynamicMessageBuilder = deviceSessionCtx.getRpcRequestDynamicMessageBuilder();
        if (rpcRequestDynamicMessageBuilder == null) {
            throw new AdaptorException("Failed to get rpcRequestDynamicMessageBuilder!");
        } else {
            return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcRequest.getRequestId(), ProtoConverter.convertToRpcRequest(rpcRequest, rpcRequestDynamicMessageBuilder)));
        }
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase) {
        return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcResponse.getRequestId(), rpcResponse.toByteArray()));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.AttributeUpdateNotificationMsg notificationMsg, String topic) {
        return Optional.of(createMqttPublishMsg(ctx, topic, notificationMsg.toByteArray()));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.ProvisionDeviceResponseMsg provisionResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, provisionResponse.toByteArray()));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, @NotNull OtaPackageType firmwareType) throws AdaptorException {
        return Optional.of(createMqttPublishMsg(ctx, String.format(MqttTopics.DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT, firmwareType.getKeyPrefix(), requestId, chunk), firmwareChunk));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            TransportApiProtos.GatewayAttributeResponseMsg.Builder responseMsgBuilder = TransportApiProtos.GatewayAttributeResponseMsg.newBuilder();
            responseMsgBuilder.setDeviceName(deviceName);
            responseMsgBuilder.setResponseMsg(responseMsg);
            byte[] payloadBytes = responseMsgBuilder.build().toByteArray();
            return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, payloadBytes));
        }
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) {
        TransportApiProtos.GatewayAttributeUpdateNotificationMsg.Builder builder = TransportApiProtos.GatewayAttributeUpdateNotificationMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setNotificationMsg(notificationMsg);
        byte[] payloadBytes = builder.build().toByteArray();
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, payloadBytes));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        TransportApiProtos.GatewayDeviceRpcRequestMsg.Builder builder = TransportApiProtos.GatewayDeviceRpcRequestMsg.newBuilder();
        builder.setDeviceName(deviceName);
        builder.setRpcRequestMsg(rpcRequest);
        byte[] payloadBytes = builder.build().toByteArray();
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC, payloadBytes));
    }

    public static byte[] toBytes(@NotNull ByteBuf inbound) {
        @NotNull byte[] bytes = new byte[inbound.readableBytes()];
        int readerIndex = inbound.readerIndex();
        inbound.getBytes(readerIndex, bytes);
        return bytes;
    }

    private int getRequestId(@NotNull String topicName, @NotNull String topic) {
        return Integer.parseInt(topicName.substring(topic.length()));
    }
}
