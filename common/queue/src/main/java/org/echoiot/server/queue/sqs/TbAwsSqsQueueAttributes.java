package org.echoiot.server.queue.sqs;

import com.amazonaws.services.sqs.model.QueueAttributeName;
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
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
public class TbAwsSqsQueueAttributes {
    @Value("${queue.aws-sqs.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.aws-sqs.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.aws-sqs.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.aws-sqs.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.aws-sqs.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.aws-sqs.queue-properties.ota-updates:}")
    private String otaProperties;
    @Value("${queue.aws-sqs.queue-properties.version-control:}")
    private String vcProperties;

    @Getter
    private Map<String, String> coreAttributes;
    @Getter
    private Map<String, String> ruleEngineAttributes;
    @Getter
    private Map<String, String> transportApiAttributes;
    @Getter
    private Map<String, String> notificationsAttributes;
    @Getter
    private Map<String, String> jsExecutorAttributes;
    @Getter
    private Map<String, String> otaAttributes;
    @Getter
    private Map<String, String> vcAttributes;

    private final Map<String, String> defaultAttributes = new HashMap<>();

    @PostConstruct
    private void init() {
        defaultAttributes.put(QueueAttributeName.FifoQueue.toString(), "true");

        coreAttributes = getConfigs(coreProperties);
        ruleEngineAttributes = getConfigs(ruleEngineProperties);
        transportApiAttributes = getConfigs(transportApiProperties);
        notificationsAttributes = getConfigs(notificationsProperties);
        jsExecutorAttributes = getConfigs(jsExecutorProperties);
        otaAttributes = getConfigs(otaProperties);
        vcAttributes = getConfigs(vcProperties);
    }

    @NotNull
    private Map<String, String> getConfigs(@NotNull String properties) {
        @NotNull Map<String, String> configs = new HashMap<>(defaultAttributes);
        if (StringUtils.isNotEmpty(properties)) {
            for (@NotNull String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                @NotNull String key = property.substring(0, delimiterPosition);
                @NotNull String value = property.substring(delimiterPosition + 1);
                validateAttributeName(key);
                configs.put(key, value);
            }
        }
        return configs;
    }

    private void validateAttributeName(@NotNull String key) {
        QueueAttributeName.fromValue(key);
    }
}
