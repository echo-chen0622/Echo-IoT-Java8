package org.echoiot.server.service.install;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.queue.settings.TbRuleEngineQueueConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Data
@EnableAutoConfiguration
@Configuration
@ConfigurationProperties(prefix = "queue.rule-engine")
@Profile("install")
public class TbRuleEngineQueueConfigService {

    private String topic;
    private List<TbRuleEngineQueueConfiguration> queues;

    @PostConstruct
    public void validate() {
        queues.stream().filter(queue -> queue.getName().equals(DataConstants.MAIN_QUEUE_NAME)).findFirst().orElseThrow(() -> {
            log.error("Main queue is not configured in echoiot.yml");
            return new RuntimeException("No \"Main\" queue configured!");
        });
    }

}
