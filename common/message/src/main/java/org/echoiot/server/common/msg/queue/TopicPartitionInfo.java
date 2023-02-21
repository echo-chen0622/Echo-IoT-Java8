package org.echoiot.server.common.msg.queue;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@ToString
public class TopicPartitionInfo {

    private final String topic;
    private final TenantId tenantId;
    private final Integer partition;
    @Getter
    private final String fullTopicName;
    @Getter
    private final boolean myPartition;

    @Builder
    public TopicPartitionInfo(String topic, @Nullable TenantId tenantId, @Nullable Integer partition, boolean myPartition) {
        this.topic = topic;
        this.tenantId = tenantId;
        this.partition = partition;
        this.myPartition = myPartition;
        String tmp = topic;
        if (tenantId != null && !tenantId.isNullUid()) {
            tmp += "." + tenantId.getId().toString();
        }
        if (partition != null) {
            tmp += "." + partition;
        }
        this.fullTopicName = tmp;
    }

    @NotNull
    public TopicPartitionInfo newByTopic(String topic) {
        return new TopicPartitionInfo(topic, this.tenantId, this.partition, this.myPartition);
    }

    public String getTopic() {
        return topic;
    }

    @NotNull
    public Optional<TenantId> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    @NotNull
    public Optional<Integer> getPartition() {
        return Optional.ofNullable(partition);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @NotNull TopicPartitionInfo that = (TopicPartitionInfo) o;
        return topic.equals(that.topic) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(partition, that.partition) &&
                fullTopicName.equals(that.fullTopicName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullTopicName);
    }
}
