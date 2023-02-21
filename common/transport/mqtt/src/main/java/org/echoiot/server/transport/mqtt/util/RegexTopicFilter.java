package org.echoiot.server.transport.mqtt.util;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@Data
public class RegexTopicFilter implements MqttTopicFilter {

    @NotNull
    private final Pattern regex;

    public RegexTopicFilter(@NotNull String regex) {
        this.regex = Pattern.compile(regex);
    }

    @Override
    public boolean filter(@NotNull String topic) {
        return regex.matcher(topic).matches();
    }
}
