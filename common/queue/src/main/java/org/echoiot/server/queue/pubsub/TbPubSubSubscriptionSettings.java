package org.echoiot.server.queue.pubsub;

import lombok.Getter;
import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
public class TbPubSubSubscriptionSettings {
    @Value("${queue.pubsub.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.pubsub.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.pubsub.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.pubsub.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.pubsub.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.pubsub.queue-properties.version-control:}")
    private String vcProperties;

    @Getter
    private Map<String, String> coreSettings;
    @Getter
    private Map<String, String> ruleEngineSettings;
    @Getter
    private Map<String, String> transportApiSettings;
    @Getter
    private Map<String, String> notificationsSettings;
    @Getter
    private Map<String, String> jsExecutorSettings;
    @Getter
    private Map<String, String> vcSettings;

    @PostConstruct
    private void init() {
        coreSettings = getSettings(coreProperties);
        ruleEngineSettings = getSettings(ruleEngineProperties);
        transportApiSettings = getSettings(transportApiProperties);
        notificationsSettings = getSettings(notificationsProperties);
        jsExecutorSettings = getSettings(jsExecutorProperties);
        vcSettings = getSettings(vcProperties);
    }

    @NotNull
    private Map<String, String> getSettings(@NotNull String properties) {
        @NotNull Map<String, String> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (@NotNull String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                @NotNull String key = property.substring(0, delimiterPosition);
                @NotNull String value = property.substring(delimiterPosition + 1);
                configs.put(key, value);
            }
        }
        return configs;
    }
}
