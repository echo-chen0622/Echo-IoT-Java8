package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;

import java.util.Collection;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineAlarmService {

    Alarm createOrUpdateAlarm(Alarm alarm);

    Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Boolean> ackAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    ListenableFuture<Boolean> clearAlarm(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTs);

    ListenableFuture<AlarmOperationResult> clearAlarmForResult(TenantId tenantId, AlarmId alarmId, JsonNode details, long clearTs);

    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<Alarm> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId);

    ListenableFuture<PageData<AlarmInfo>> findAlarms(TenantId tenantId, AlarmQuery query);

    ListenableFuture<PageData<AlarmInfo>> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus);

    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds);
}
