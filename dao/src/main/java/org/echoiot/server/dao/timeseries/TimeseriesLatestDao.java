package org.echoiot.server.dao.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.DeleteTsKvQuery;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.kv.TsKvLatestRemovingResult;

import java.util.List;
import java.util.Optional;

public interface TimeseriesLatestDao {

    /**
     * Optional TsKvEntry if the value is present in the DB
     *
     */
    ListenableFuture<Optional<TsKvEntry>> findLatestOpt(TenantId tenantId, EntityId entityId, String key);

    /**
     * Returns new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null)) if the value is NOT present in the DB
     *
     */
    ListenableFuture<TsKvEntry> findLatest(TenantId tenantId, EntityId entityId, String key);

    ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId);

    ListenableFuture<Void> saveLatest(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry);

    ListenableFuture<TsKvLatestRemovingResult> removeLatest(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query);

    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds);
}
