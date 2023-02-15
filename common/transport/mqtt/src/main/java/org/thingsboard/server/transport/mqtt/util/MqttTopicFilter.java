package org.thingsboard.server.transport.mqtt.util;

public interface MqttTopicFilter {

    boolean filter(String topic);

}
