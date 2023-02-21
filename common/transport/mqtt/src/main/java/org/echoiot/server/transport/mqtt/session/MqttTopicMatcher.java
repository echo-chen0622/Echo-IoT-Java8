package org.echoiot.server.transport.mqtt.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class MqttTopicMatcher {

    @NotNull
    private final String topic;
    @NotNull
    private final Pattern topicRegex;

    public MqttTopicMatcher(@NotNull String topic) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        this.topic = topic;
        this.topicRegex = Pattern.compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    }

    public String getTopic() {
        return topic;
    }

    public boolean matches(@NotNull String topic){
        return this.topicRegex.matcher(topic).matches();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @NotNull MqttTopicMatcher that = (MqttTopicMatcher) o;

        return topic.equals(that.topic);
    }

    @Override
    public int hashCode() {
        return topic.hashCode();
    }
}
