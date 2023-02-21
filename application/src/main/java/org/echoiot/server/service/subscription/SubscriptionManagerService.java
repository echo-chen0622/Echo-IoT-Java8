package org.echoiot.server.service.subscription;

import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;

public interface SubscriptionManagerService extends ApplicationListener<PartitionChangeEvent> {

    void addSubscription(TbSubscription subscription, TbCallback callback);

    void cancelSubscription(String sessionId, int subscriptionId, TbCallback callback);

    void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback);

    void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback);

    void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, TbCallback callback);

    void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, TbCallback empty);

    void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback);

    void onAlarmUpdate(TenantId tenantId, EntityId entityId, Alarm alarm, TbCallback callback);

    void onAlarmDeleted(TenantId tenantId, EntityId entityId, Alarm alarm, TbCallback callback);


}
