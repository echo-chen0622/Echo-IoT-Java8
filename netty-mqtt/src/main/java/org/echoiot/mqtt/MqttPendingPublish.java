package org.echoiot.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

final class MqttPendingPublish {

    private final int messageId;
    private final Promise<Void> future;
    private final ByteBuf payload;
    private final MqttPublishMessage message;
    private final MqttQoS qos;

    @NotNull
    private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler;
    @NotNull
    private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler;

    private boolean sent = false;

    MqttPendingPublish(int messageId, Promise<Void> future, ByteBuf payload, MqttPublishMessage message, MqttQoS qos, PendingOperation operation) {
        this.messageId = messageId;
        this.future = future;
        this.payload = payload;
        this.message = message;
        this.qos = qos;

        this.publishRetransmissionHandler = new RetransmissionHandler<>(operation);
        this.publishRetransmissionHandler.setOriginalMessage(message);
        this.pubrelRetransmissionHandler = new RetransmissionHandler<>(operation);
    }

    int getMessageId() {
        return messageId;
    }

    Promise<Void> getFuture() {
        return future;
    }

    ByteBuf getPayload() {
        return payload;
    }

    boolean isSent() {
        return sent;
    }

    void setSent(boolean sent) {
        this.sent = sent;
    }

    MqttPublishMessage getMessage() {
        return message;
    }

    MqttQoS getQos() {
        return qos;
    }

    void startPublishRetransmissionTimer(EventLoop eventLoop, @NotNull Consumer<Object> sendPacket) {
        this.publishRetransmissionHandler.setHandle(((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttPublishMessage(fixedHeader, originalMessage.variableHeader(), this.payload.retain()))));
        this.publishRetransmissionHandler.start(eventLoop);
    }

    void onPubackReceived() {
        this.publishRetransmissionHandler.stop();
    }

    void setPubrelMessage(MqttMessage pubrelMessage) {
        this.pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
    }

    void startPubrelRetransmissionTimer(EventLoop eventLoop, @NotNull Consumer<Object> sendPacket) {
        this.pubrelRetransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.pubrelRetransmissionHandler.start(eventLoop);
    }

    void onPubcompReceived() {
        this.pubrelRetransmissionHandler.stop();
    }

    void onChannelClosed() {
        this.publishRetransmissionHandler.stop();
        this.pubrelRetransmissionHandler.stop();
        if (payload != null) {
            payload.release();
        }
    }
}
