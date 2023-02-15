package org.thingsboard.server.queue.azure.servicebus;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
@Component
@Data
public class TbServiceBusSettings {
    @Value("${queue.service_bus.namespace_name}")
    private String namespaceName;
    @Value("${queue.service_bus.sas_key_name}")
    private String sasKeyName;
    @Value("${queue.service_bus.sas_key}")
    private String sasKey;
    @Value("${queue.service_bus.max_messages}")
    private int maxMessages;
}
