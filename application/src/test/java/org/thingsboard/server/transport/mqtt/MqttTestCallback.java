package org.thingsboard.server.transport.mqtt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
public class MqttTestCallback implements MqttCallback {

    protected CountDownLatch subscribeLatch;
    protected final CountDownLatch deliveryLatch;
    protected int qoS;
    protected byte[] payloadBytes;
    protected String awaitSubTopic;
    protected boolean pubAckReceived;

    public MqttTestCallback() {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttTestCallback(int subscribeCount) {
        this.subscribeLatch = new CountDownLatch(subscribeCount);
        this.deliveryLatch = new CountDownLatch(1);
    }

    public MqttTestCallback(String awaitSubTopic) {
        this.subscribeLatch = new CountDownLatch(1);
        this.deliveryLatch = new CountDownLatch(1);
        this.awaitSubTopic = awaitSubTopic;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        log.warn("connectionLost: ", throwable);
        deliveryLatch.countDown();
    }

    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        if (awaitSubTopic == null) {
            log.warn("messageArrived on topic: {}", requestTopic);
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        } else {
            messageArrivedOnAwaitSubTopic(requestTopic, mqttMessage);
        }
    }

    protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            qoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        log.warn("delivery complete: {}", iMqttDeliveryToken.getResponse());
        pubAckReceived = iMqttDeliveryToken.getResponse().getType() == MqttWireMessage.MESSAGE_TYPE_PUBACK;
        deliveryLatch.countDown();

    }
}
