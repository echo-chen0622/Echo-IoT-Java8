package org.echoiot.server.common.data.queue;

import lombok.Data;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;

@Data
public class Queue extends SearchTextBasedWithAdditionalInfo<QueueId> implements HasName, HasTenantId {
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "topic")
    private String topic;
    private int pollInterval;
    private int partitions;
    private boolean consumerPerPartition;
    private long packProcessingTimeout;
    private SubmitStrategy submitStrategy;
    private ProcessingStrategy processingStrategy;

    public Queue() {
    }

    public Queue(QueueId id) {
        super(id);
    }

    public Queue(TenantId tenantId, TenantProfileQueueConfiguration queueConfiguration) {
        this.tenantId = tenantId;
        this.name = queueConfiguration.getName();
        this.topic = queueConfiguration.getTopic();
        this.pollInterval = queueConfiguration.getPollInterval();
        this.partitions = queueConfiguration.getPartitions();
        this.consumerPerPartition = queueConfiguration.isConsumerPerPartition();
        this.packProcessingTimeout = queueConfiguration.getPackProcessingTimeout();
        this.submitStrategy = queueConfiguration.getSubmitStrategy();
        this.processingStrategy = queueConfiguration.getProcessingStrategy();
        setAdditionalInfo(queueConfiguration.getAdditionalInfo());
    }

    @Override
    public String getSearchText() {
        return getName();
    }
}
