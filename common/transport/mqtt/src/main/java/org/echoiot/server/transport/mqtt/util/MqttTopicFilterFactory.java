package org.echoiot.server.transport.mqtt.util;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.device.profile.MqttTopics;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MqttTopicFilterFactory {

    private static final ConcurrentMap<String, MqttTopicFilter> filters = new ConcurrentHashMap<>();
    private static final MqttTopicFilter DEFAULT_TELEMETRY_TOPIC_FILTER = toFilter(MqttTopics.DEVICE_TELEMETRY_TOPIC);
    private static final MqttTopicFilter DEFAULT_ATTRIBUTES_TOPIC_FILTER = toFilter(MqttTopics.DEVICE_ATTRIBUTES_TOPIC);

    @NotNull
    public static MqttTopicFilter toFilter(@NotNull String topicFilter) {
        if (topicFilter == null || topicFilter.isEmpty()) {
            throw new IllegalArgumentException("Topic filter can't be empty!");
        }
        return filters.computeIfAbsent(topicFilter, filter -> {
            if (filter.equals("#")) {
                return new AlwaysTrueTopicFilter();
            } else if (filter.contains("+") || filter.contains("#")) {
                @NotNull String regex = filter
                        .replace("\\", "\\\\")
                        .replace("+", "[^/]+")
                        .replace("/#", "($|/.*)");
                log.debug("Converting [{}] to [{}]", filter, regex);
                return new RegexTopicFilter(regex);
            } else {
                return new EqualsTopicFilter(filter);
            }
        });
    }

    public static MqttTopicFilter getDefaultTelemetryFilter() {
        return DEFAULT_TELEMETRY_TOPIC_FILTER;
    }

    public static MqttTopicFilter getDefaultAttributesFilter() {
        return DEFAULT_ATTRIBUTES_TOPIC_FILTER;
    }
}
