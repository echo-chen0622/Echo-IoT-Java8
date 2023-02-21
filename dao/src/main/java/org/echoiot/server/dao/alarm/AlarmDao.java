package org.echoiot.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmInfo;
import org.echoiot.server.common.data.alarm.AlarmQuery;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.alarm.EntityAlarm;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.common.data.query.AlarmDataQuery;
import org.echoiot.server.dao.Dao;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Echo on 11.05.17.
 */
public interface AlarmDao extends Dao<Alarm> {

    @Nullable
    Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    ListenableFuture<Alarm> findLatestByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type);

    Alarm findAlarmById(TenantId tenantId, UUID key);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key);

    Alarm save(TenantId tenantId, Alarm alarm);

    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, Set<AlarmStatus> status);

    PageData<AlarmId> findAlarmsIdsByEndTsBeforeAndTenantId(Long time, TenantId tenantId, PageLink pageLink);

    void createEntityAlarmRecord(EntityAlarm entityAlarm);

    List<EntityAlarm> findEntityAlarmRecords(TenantId tenantId, AlarmId id);

    void deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId);
}
