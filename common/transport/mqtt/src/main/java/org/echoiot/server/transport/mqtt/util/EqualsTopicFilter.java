package org.echoiot.server.transport.mqtt.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class EqualsTopicFilter implements MqttTopicFilter {

    @NotNull
    private final String filter;

    @Override
    public boolean filter(String topic) {
        return filter.equals(topic);
    }
}
