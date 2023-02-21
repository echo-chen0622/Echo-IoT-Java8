package org.echoiot.mqtt.integration.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.mqtt.MqttMessageType.*;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    private final List<MqttMessageType> eventsFromClient;
    @NotNull
    private final UUID sessionId;

    MqttTransportHandler(List<MqttMessageType> eventsFromClient) {
        this.sessionId = UUID.randomUUID();
        this.eventsFromClient = eventsFromClient;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        try {
            if (msg instanceof MqttMessage) {
                @NotNull MqttMessage message = (MqttMessage) msg;
                if (message.decoderResult().isSuccess()) {
                    processMqttMsg(ctx, message);
                } else {
                    log.error("[{}] Message decoding failed: {}", sessionId, message.decoderResult().cause().getMessage());
                    ctx.close();
                }
            } else {
                log.debug("[{}] Received non mqtt message: {}", sessionId, msg.getClass().getSimpleName());
                ctx.close();
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    void processMqttMsg(@NotNull ChannelHandlerContext ctx, @NotNull MqttMessage msg) {
        if (msg.fixedHeader() == null) {
            ctx.close();
            return;
        }
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                eventsFromClient.add(CONNECT);
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
            case DISCONNECT:
                eventsFromClient.add(DISCONNECT);
                ctx.close();
                break;
            case PUBLISH:
                // QoS 0 and 1 supported only here
                eventsFromClient.add(PUBLISH);
                @NotNull MqttPublishMessage mqttPubMsg = (MqttPublishMessage) msg;
                ack(ctx, mqttPubMsg.variableHeader().packetId());
                break;
            case PINGREQ:
                // We will not handle PINGREQ and will not send any PINGRESP to simulate the MQTT server is down
                eventsFromClient.add(PINGREQ);
                break;
            default:
                break;
        }
    }

    void processConnect(@NotNull ChannelHandlerContext ctx, @NotNull MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        String clientId = msg.payload().clientIdentifier();

        log.warn("[{}][{}] Processing connect msg for client: {}!", sessionId, userName, clientId);
        ctx.writeAndFlush(createMqttConnAckMsg(msg));
    }

    @NotNull
    private MqttConnAckMessage createMqttConnAckMsg(@NotNull MqttConnectMessage msg) {
        @NotNull MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(CONNACK, false, AT_MOST_ONCE, false, 0);
        @NotNull MqttConnAckVariableHeader mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, !msg.variableHeader().isCleanSession());
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    private void ack(@NotNull ChannelHandlerContext ctx, int msgId) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(msgId));
        }
    }

    @NotNull
    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        @NotNull MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_MOST_ONCE, false, 0);
        @NotNull MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    @Override
    public void operationComplete(Future<? super Void> future) {
        log.trace("[{}] Channel closed!", sessionId);
    }
}
