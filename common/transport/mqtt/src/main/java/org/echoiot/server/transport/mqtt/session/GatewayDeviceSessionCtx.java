package org.echoiot.server.transport.mqtt.session;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttMessage;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.rpc.RpcStatus;
import org.echoiot.server.common.transport.SessionMsgListener;
import org.echoiot.server.common.transport.TransportService;
import org.echoiot.server.common.transport.TransportServiceCallback;
import org.echoiot.server.common.transport.auth.TransportDeviceInfo;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.gen.transport.TransportProtos.SessionInfoProto;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Echo on 19.01.17.
 */
@Slf4j
public class GatewayDeviceSessionCtx extends MqttDeviceAwareSessionContext implements SessionMsgListener {

    private final GatewaySessionHandler parent;
    private final TransportService transportService;

    public GatewayDeviceSessionCtx(@NotNull GatewaySessionHandler parent, @NotNull TransportDeviceInfo deviceInfo,
                                   DeviceProfile deviceProfile, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                   TransportService transportService) {
        super(UUID.randomUUID(), mqttQoSMap);
        this.parent = parent;
        setSessionInfo(SessionInfoProto.newBuilder()
                .setNodeId(parent.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(deviceInfo.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceInfo.getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(deviceInfo.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(deviceInfo.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(deviceInfo.getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(deviceInfo.getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(deviceInfo.getDeviceName())
                .setDeviceType(deviceInfo.getDeviceType())
                .setGwSessionIdMSB(parent.getSessionId().getMostSignificantBits())
                .setGwSessionIdLSB(parent.getSessionId().getLeastSignificantBits())
                .setDeviceProfileIdMSB(deviceInfo.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(deviceInfo.getDeviceProfileId().getId().getLeastSignificantBits())
                .build());
        setDeviceInfo(deviceInfo);
        setConnected(true);
        setDeviceProfile(deviceProfile);
        this.transportService = transportService;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public int nextMsgId() {
        return parent.nextMsgId();
    }

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), response).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), notification).ifPresent(parent::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, @NotNull TransportProtos.ToDeviceRpcRequestMsg request) {
        log.trace("[{}] Received RPC command to device", sessionId);
        try {
            parent.getPayloadAdaptor().convertToGatewayPublish(this, getDeviceInfo().getDeviceName(), request).ifPresent(
                    payload -> {
                        ChannelFuture channelFuture = parent.writeAndFlush(payload);
                        if (request.getPersisted()) {
                            channelFuture.addListener(result -> {
                                if (result.cause() == null) {
                                    if (!isAckExpected(payload)) {
                                        transportService.process(getSessionInfo(), request, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
                                    } else if (request.getPersisted()) {
                                        transportService.process(getSessionInfo(), request, RpcStatus.SENT, TransportServiceCallback.EMPTY);

                                    }
                                }
                            });
                        }
                    }
            );
        } catch (Exception e) {
            transportService.process(getSessionInfo(),
                    TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                            .setRequestId(request.getRequestId()).setError("Failed to convert device RPC command to MQTT msg").build(), TransportServiceCallback.EMPTY);
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, @NotNull TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        parent.deregisterSession(getDeviceInfo().getDeviceName());
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        // This feature is not supported in the TB IoT Gateway yet.
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        parent.onDeviceDeleted(this.getSessionInfo().getDeviceName());
    }

    private boolean isAckExpected(@NotNull MqttMessage message) {
        return message.fixedHeader().qosLevel().value() > 0;
    }

}
