package org.echoiot.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQueryResult;

public interface AggregationTimeseriesDao {

    ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query);
}
