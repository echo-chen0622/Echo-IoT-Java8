package org.echoiot.server.dao.timeseries;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.EntityId;

@Data
@AllArgsConstructor
public class CassandraPartitionCacheKey {

    private EntityId entityId;
    private String key;
    private long partition;

}
