package org.thingsboard.server.common.data.tenant.profile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategy;

@Data
public class TenantProfileQueueConfiguration {
    private String name;
    private String topic;
    private int pollInterval;
    private int partitions;
    private boolean consumerPerPartition;
    private long packProcessingTimeout;
    private SubmitStrategy submitStrategy;
    private ProcessingStrategy processingStrategy;
    private JsonNode additionalInfo;
}
