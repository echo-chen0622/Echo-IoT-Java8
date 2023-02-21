package org.echoiot.mqtt;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

final class MqttPendingUnsubscription{

    private final Promise<Void> future;
    private final String topic;

    @NotNull
    private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler;

    MqttPendingUnsubscription(Promise<Void> future, String topic, MqttUnsubscribeMessage unsubscribeMessage, PendingOperation operation) {
        this.future = future;
        this.topic = topic;

        this.retransmissionHandler = new RetransmissionHandler<>(operation);
        this.retransmissionHandler.setOriginalMessage(unsubscribeMessage);
    }

    Promise<Void> getFuture() {
        return future;
    }

    String getTopic() {
        return topic;
    }

    void startRetransmissionTimer(@NotNull EventLoop eventLoop, @NotNull Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttUnsubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
        this.retransmissionHandler.start(eventLoop);
    }

    void onUnsubackReceived(){
        this.retransmissionHandler.stop();
    }

    void onChannelClosed(){
        this.retransmissionHandler.stop();
    }
}
