package org.thingsboard.server.common.data.queue;

import lombok.Data;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

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
