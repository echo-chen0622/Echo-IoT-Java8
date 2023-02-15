package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

public interface AggregationTimeseriesDao {

    ListenableFuture<ReadTsKvQueryResult> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query);
}
