package org.echoiot.server.transport.mqtt.adaptors;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.mqtt.session.MqttDeviceAwareSessionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Data
@AllArgsConstructor
@Slf4j
public class BackwardCompatibilityAdaptor implements MqttTransportAdaptor {

    private MqttTransportAdaptor protoAdaptor;
    private MqttTransportAdaptor jsonAdaptor;

    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return protoAdaptor.convertToPostTelemetry(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process post telemetry request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToPostTelemetry(ctx, inbound);
        }
    }

    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return protoAdaptor.convertToPostAttributes(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process post attributes request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToPostAttributes(ctx, inbound);
        }
    }

    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topicBase) throws AdaptorException {
        try {
            return protoAdaptor.convertToGetAttributes(ctx, inbound, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process get attributes request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToGetAttributes(ctx, inbound, topicBase);
        }
    }

    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        try {
            return protoAdaptor.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process to device rpc response msg: {} due to: ", ctx.getSessionId(), mqttMsg, e);
            return jsonAdaptor.convertToDeviceRpcResponse(ctx, mqttMsg, topicBase);
        }
    }

    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException {
        try {
            return protoAdaptor.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process to server rpc request msg: {} due to: ", ctx.getSessionId(), mqttMsg, e);
            return jsonAdaptor.convertToServerRpcRequest(ctx, mqttMsg, topicBase);
        }
    }

    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        try {
            return protoAdaptor.convertToClaimDevice(ctx, inbound);
        } catch (AdaptorException e) {
            log.trace("[{}] failed to process claim device request msg: {} due to: ", ctx.getSessionId(), inbound, e);
            return jsonAdaptor.convertToClaimDevice(ctx, inbound);
        }
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! GetAttributeResponseMsg: {} TopicBase: {}", ctx.getSessionId(), responseMsg, topicBase);
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException {
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, responseMsg);
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.AttributeUpdateNotificationMsg notificationMsg, String topic) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! AttributeUpdateNotificationMsg: {} Topic: {}", ctx.getSessionId(), notificationMsg, topic);
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException {
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, notificationMsg);
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, String topicBase) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! ToDeviceRpcRequestMsg: {} TopicBase: {}", ctx.getSessionId(), rpcRequest, topicBase);
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException {
        return protoAdaptor.convertToGatewayPublish(ctx, deviceName, rpcRequest);
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.ToServerRpcResponseMsg rpcResponse, String topicBase) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! ToServerRpcResponseMsg: {} TopicBase: {}", ctx.getSessionId(), rpcResponse, topicBase);
        return Optional.empty();
    }

    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(@NotNull MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! MqttPublishMessage: {}", ctx.getSessionId(), inbound);
        return null;
    }

    @NotNull
    @Override
    public Optional<MqttMessage> convertToPublish(@NotNull MqttDeviceAwareSessionContext ctx, TransportProtos.ProvisionDeviceResponseMsg provisionResponse) throws AdaptorException {
        log.warn("[{}] invoked not implemented adaptor method! ProvisionDeviceResponseMsg: {}", ctx.getSessionId(), provisionResponse);
        return Optional.empty();
    }

    @Override
    public Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, OtaPackageType firmwareType) throws AdaptorException {
        return protoAdaptor.convertToPublish(ctx, firmwareChunk, requestId, chunk, firmwareType);
    }
}
