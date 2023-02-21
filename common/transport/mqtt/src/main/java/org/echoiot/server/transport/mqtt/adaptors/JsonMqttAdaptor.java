package org.echoiot.server.transport.mqtt.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.profile.MqttTopics;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.mqtt.session.MqttDeviceAwareSessionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
public class JsonMqttAdaptor implements MqttTransportAdaptor {

    protected static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            log.debug("Failed to decode post telemetry request", ex);
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            log.debug("Failed to decode post attributes request", ex);
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(ctx.getSessionId(), inbound.payload(), true);
        try {
            return JsonConverter.convertToClaimDeviceProto(ctx.getDeviceId(), payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            log.debug("Failed to decode claim device request", ex);
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound) throws AdaptorException {
        @Nullable String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            return JsonConverter.convertToProvisionRequestMsg(payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        return processGetAttributeRequestMsg(inbound, topicBase);
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        return processToDeviceRpcResponseMsg(inbound, topicBase);
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        return processToServerRpcRequestMsg(ctx, inbound, topicBase);
    }

    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException {
        return processConvertFromAttributeResponseMsg(ctx, responseMsg, topicBase);
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return processConvertFromGatewayAttributeResponseMsg(ctx, deviceName, responseMsg);
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.AttributeUpdateNotificationMsg notificationMsg, @NotNull String topic) {
        return Optional.of(createMqttPublishMsg(ctx, topic, JsonConverter.toJson(notificationMsg)));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) {
        @NotNull JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, notificationMsg);
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_TOPIC, result));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.ToDeviceRpcRequestMsg rpcRequest, String topicBase) {
        return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcRequest.getRequestId(), JsonConverter.toJson(rpcRequest, false)));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToGatewayPublish(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_RPC_TOPIC, JsonConverter.toGatewayJson(deviceName, rpcRequest)));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase) {
        return Optional.of(createMqttPublishMsg(ctx, topicBase + rpcResponse.getRequestId(), JsonConverter.toJson(rpcResponse)));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.ProvisionDeviceResponseMsg provisionResponse) {
        return Optional.of(createMqttPublishMsg(ctx, MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC, JsonConverter.toJson(provisionResponse)));
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, @NotNull OtaPackageType firmwareType) {
        return Optional.of(createMqttPublishMsg(ctx, String.format(MqttTopics.DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT, firmwareType.getKeyPrefix(), requestId, chunk), firmwareChunk));
    }

    public static JsonElement validateJsonPayload(UUID sessionId, @NotNull ByteBuf payloadData) throws AdaptorException {
        @Nullable String payload = validatePayload(sessionId, payloadData, false);
        try {
            return new JsonParser().parse(payload);
        } catch (JsonSyntaxException ex) {
            log.debug("Payload is in incorrect format: {}", payload);
            throw new AdaptorException(ex);
        }
    }

    private TransportProtos.GetAttributeRequestMsg processGetAttributeRequestMsg(@NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
            result.setRequestId(getRequestId(topicName, topicBase));
            String payload = inbound.payload().toString(UTF8);
            JsonElement requestBody = new JsonParser().parse(payload);
            @Nullable Set<String> clientKeys = toStringSet(requestBody, "clientKeys");
            @Nullable Set<String> sharedKeys = toStringSet(requestBody, "sharedKeys");
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
            return result.build();
        } catch (RuntimeException e) {
            log.debug("Failed to decode get attributes request", e);
            throw new AdaptorException(e);
        }
    }

    private TransportProtos.ToDeviceRpcResponseMsg processToDeviceRpcResponseMsg(@NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        try {
            int requestId = getRequestId(topicName, topicBase);
            String payload = inbound.payload().toString(UTF8);
            return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(payload).build();
        } catch (RuntimeException e) {
            log.debug("Failed to decode rpc response", e);
            throw new AdaptorException(e);
        }
    }

    private TransportProtos.ToServerRpcRequestMsg processToServerRpcRequestMsg(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull MqttPublishMessage inbound, @NotNull String topicBase) throws AdaptorException {
        String topicName = inbound.variableHeader().topicName();
        @Nullable String payload = validatePayload(ctx.getSessionId(), inbound.payload(), false);
        try {
            int requestId = getRequestId(topicName, topicBase);
            return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), requestId);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            log.debug("Failed to decode to server rpc request", ex);
            throw new AdaptorException(ex);
        }
    }

    @NotNull
    private Optional<MqttMessage> processConvertFromAttributeResponseMsg(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            int requestId = responseMsg.getRequestId();
            if (requestId >= 0) {
                return Optional.of(createMqttPublishMsg(ctx,
                        topicBase + requestId,
                        JsonConverter.toJson(responseMsg)));
            }
            return Optional.empty();
        }
    }

    @NotNull
    private Optional<MqttMessage> processConvertFromGatewayAttributeResponseMsg(@NotNull MqttDeviceAwareSessionContext ctx, String deviceName, @NotNull TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        if (!StringUtils.isEmpty(responseMsg.getError())) {
            throw new AdaptorException(responseMsg.getError());
        } else {
            @NotNull JsonObject result = JsonConverter.getJsonObjectForGateway(deviceName, responseMsg);
            return Optional.of(createMqttPublishMsg(ctx, MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC, result));
        }
    }

    @NotNull
    protected MqttPublishMessage createMqttPublishMsg(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull String topic, @NotNull JsonElement json) {
        @NotNull MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        @NotNull MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(json.toString().getBytes(UTF8));
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }

    @Nullable
    private Set<String> toStringSet(@NotNull JsonElement requestBody, String name) {
        JsonElement element = requestBody.getAsJsonObject().get(name);
        if (element != null) {
            return new HashSet<>(Arrays.asList(element.getAsString().split(",")));
        } else {
            return null;
        }
    }

    @Nullable
    private static String validatePayload(UUID sessionId, @NotNull ByteBuf payloadData, boolean isEmptyPayloadAllowed) throws AdaptorException {
        String payload = payloadData.toString(UTF8);
        if (payload == null) {
            log.debug("[{}] Payload is empty!", sessionId);
            if (!isEmptyPayloadAllowed) {
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
        }
        return payload;
    }

    private int getRequestId(@NotNull String topicName, @NotNull String topic) {
        return Integer.parseInt(topicName.substring(topic.length()));
    }

}
