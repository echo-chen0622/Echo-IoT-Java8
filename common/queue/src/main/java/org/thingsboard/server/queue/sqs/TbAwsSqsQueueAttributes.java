package org.thingsboard.server.queue.sqs;

import com.amazonaws.services.sqs.model.QueueAttributeName;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

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

    private Map<String, String> getConfigs(String properties) {
        Map<String, String> configs = new HashMap<>(defaultAttributes);
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String value = property.substring(delimiterPosition + 1);
                validateAttributeName(key);
                configs.put(key, value);
            }
        }
        return configs;
    }

    private void validateAttributeName(String key) {
        QueueAttributeName.fromValue(key);
    }
}
