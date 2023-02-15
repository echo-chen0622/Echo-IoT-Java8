package org.thingsboard.mqtt;

import io.netty.handler.codec.mqtt.MqttPublishMessage;

final class MqttIncomingQos2Publish {

    private final MqttPublishMessage incomingPublish;

    MqttIncomingQos2Publish(MqttPublishMessage incomingPublish) {
        this.incomingPublish = incomingPublish;
    }

    MqttPublishMessage getIncomingPublish() {
        return incomingPublish;
    }
}
