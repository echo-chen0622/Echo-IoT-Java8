package org.thingsboard.server.queue.rabbitmq;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
public class TbRabbitMqQueueArguments {
    @Value("${queue.rabbitmq.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.rabbitmq.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.rabbitmq.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.rabbitmq.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.rabbitmq.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.rabbitmq.queue-properties.version-control:}")
    private String vcProperties;

    @Getter
    private Map<String, Object> coreArgs;
    @Getter
    private Map<String, Object> ruleEngineArgs;
    @Getter
    private Map<String, Object> transportApiArgs;
    @Getter
    private Map<String, Object> notificationsArgs;
    @Getter
    private Map<String, Object> jsExecutorArgs;
    @Getter
    private Map<String, Object> vcArgs;

    @PostConstruct
    private void init() {
        coreArgs = getArgs(coreProperties);
        ruleEngineArgs = getArgs(ruleEngineProperties);
        transportApiArgs = getArgs(transportApiProperties);
        notificationsArgs = getArgs(notificationsProperties);
        jsExecutorArgs = getArgs(jsExecutorProperties);
        vcArgs = getArgs(vcProperties);
    }

    private Map<String, Object> getArgs(String properties) {
        Map<String, Object> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String strValue = property.substring(delimiterPosition + 1);
                configs.put(key, getObjectValue(strValue));
            }
        }
        return configs;
    }

    private Object getObjectValue(String str) {
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.valueOf(str);
        } else if (isNumeric(str)) {
            return getNumericValue(str);
        }
        return str;
    }

    private Object getNumericValue(String str) {
        if (str.contains(".")) {
            return Double.valueOf(str);
        } else {
            return Long.valueOf(str);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return PATTERN.matcher(strNum).matches();
    }
}
