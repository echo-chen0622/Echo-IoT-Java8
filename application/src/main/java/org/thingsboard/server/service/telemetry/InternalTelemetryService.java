package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface InternalTelemetryService extends RuleEngineTelemetryService {

    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Integer> callback);

    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Integer> callback);

    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback);

    void saveLatestAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    void deleteAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback);

    void deleteLatestInternal(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback);



}
