package org.echoiot.server.transport.mqtt.adaptors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.echoiot.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.echoiot.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.echoiot.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.echoiot.server.gen.transport.TransportProtos.ToServerRpcResponseMsg;
import org.echoiot.server.transport.mqtt.session.MqttDeviceAwareSessionContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public interface MqttTransportAdaptor {

    ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    PostAttributeMsg convertToPostAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    GetAttributeRequestMsg convertToGetAttributes(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound, String topicBase) throws AdaptorException;

    ToDeviceRpcResponseMsg convertToDeviceRpcResponse(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException;

    ToServerRpcRequestMsg convertToServerRpcRequest(MqttDeviceAwareSessionContext ctx, MqttPublishMessage mqttMsg, String topicBase) throws AdaptorException;

    ClaimDeviceMsg convertToClaimDevice(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, GetAttributeResponseMsg responseMsg, String topicBase) throws AdaptorException;

    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, GetAttributeResponseMsg responseMsg) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, AttributeUpdateNotificationMsg notificationMsg, String topic) throws AdaptorException;

    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToDeviceRpcRequestMsg rpcRequest, String topicBase) throws AdaptorException;

    Optional<MqttMessage> convertToGatewayPublish(MqttDeviceAwareSessionContext ctx, String deviceName, ToDeviceRpcRequestMsg rpcRequest) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ToServerRpcResponseMsg rpcResponse, String topicBase) throws AdaptorException;

    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, ProvisionDeviceResponseMsg provisionResponse) throws AdaptorException;

    Optional<MqttMessage> convertToPublish(MqttDeviceAwareSessionContext ctx, byte[] firmwareChunk, String requestId, int chunk, OtaPackageType firmwareType) throws AdaptorException;

    @NotNull
    default MqttPublishMessage createMqttPublishMsg(@NotNull MqttDeviceAwareSessionContext ctx, @NotNull String topic, byte[] payloadInBytes) {
        @NotNull MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBLISH, false, ctx.getQoSForTopic(topic), false, 0);
        @NotNull MqttPublishVariableHeader header = new MqttPublishVariableHeader(topic, ctx.nextMsgId());
        ByteBuf payload = ALLOCATOR.buffer();
        payload.writeBytes(payloadInBytes);
        return new MqttPublishMessage(mqttFixedHeader, header, payload);
    }
}
