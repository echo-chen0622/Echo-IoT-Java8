package org.echoiot.server.dao.timeseries;

import lombok.Getter;
import org.echoiot.server.common.data.kv.TsKvQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class QueryCursor {

    @Getter
    protected final String entityType;
    @Getter
    protected final UUID entityId;
    @Getter
    protected final String key;
    @Getter
    private final long startTs;
    @Getter
    private final long endTs;

    final List<Long> partitions;
    private int partitionIndex;

    public QueryCursor(String entityType, UUID entityId, @NotNull TsKvQuery baseQuery, @NotNull List<Long> partitions) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.key = baseQuery.getKey();
        this.startTs = baseQuery.getStartTs();
        this.endTs = baseQuery.getEndTs();
        this.partitions = partitions;
        this.partitionIndex = partitions.size() - 1;
    }

    public boolean hasNextPartition() {
        return partitionIndex >= 0;
    }

    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        partitionIndex--;
        return partition;
    }

}
