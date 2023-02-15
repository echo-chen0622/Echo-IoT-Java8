package org.thingsboard.server.queue.settings;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Data
@Component
public class TbQueueCoreSettings {

    @Value("${queue.core.topic}")
    private String topic;

    @Value("${queue.core.ota.topic:tb_ota_package}")
    private String otaPackageTopic;

    @Value("${queue.core.usage-stats-topic:tb_usage_stats}")
    private String usageStatsTopic;

    @Value("${queue.core.partitions}")
    private int partitions;
}
