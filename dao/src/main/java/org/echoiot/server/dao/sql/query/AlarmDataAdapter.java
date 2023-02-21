package org.echoiot.server.dao.sql.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.common.data.query.EntityDataPageLink;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AlarmDataAdapter {

    private final static ObjectMapper mapper = new ObjectMapper();

    @NotNull
    public static PageData<AlarmData> createAlarmData(@NotNull EntityDataPageLink pageLink,
                                                      @NotNull List<Map<String, Object>> rows,
                                                      int totalElements, @NotNull Collection<EntityId> orderedEntityIds) {
        @NotNull Map<UUID, EntityId> entityIdMap = orderedEntityIds.stream().collect(Collectors.toMap(EntityId::getId, Function.identity()));
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        @NotNull List<AlarmData> entitiesData = convertListToAlarmData(rows, entityIdMap);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    @NotNull
    private static List<AlarmData> convertListToAlarmData(@NotNull List<Map<String, Object>> result, @NotNull Map<UUID, EntityId> entityIdMap) {
        return result.stream().map(tmp -> toEntityData(tmp, entityIdMap)).collect(Collectors.toList());
    }

    @NotNull
    private static AlarmData toEntityData(@NotNull Map<String, Object> row, @NotNull Map<UUID, EntityId> entityIdMap) {
        @NotNull Alarm alarm = new Alarm();
        alarm.setId(new AlarmId((UUID) row.get(ModelConstants.ID_PROPERTY)));
        alarm.setCreatedTime((long) row.get(ModelConstants.CREATED_TIME_PROPERTY));
        alarm.setAckTs((long) row.get(ModelConstants.ALARM_ACK_TS_PROPERTY));
        alarm.setClearTs((long) row.get(ModelConstants.ALARM_CLEAR_TS_PROPERTY));
        alarm.setStartTs((long) row.get(ModelConstants.ALARM_START_TS_PROPERTY));
        alarm.setEndTs((long) row.get(ModelConstants.ALARM_END_TS_PROPERTY));
        Object additionalInfo = row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY);
        if (additionalInfo != null) {
            try {
                alarm.setDetails(mapper.readTree(additionalInfo.toString()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse json: {}", row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY), e);
            }
        }
        EntityType originatorType = EntityType.values()[(int) row.get(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY)];
        UUID originatorId = (UUID) row.get(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        alarm.setPropagate((boolean) row.get(ModelConstants.ALARM_PROPAGATE_PROPERTY));
        alarm.setPropagateToOwner((boolean) row.get(ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY));
        alarm.setPropagateToTenant((boolean) row.get(ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY));
        alarm.setType(row.get(ModelConstants.ALARM_TYPE_PROPERTY).toString());
        alarm.setSeverity(AlarmSeverity.valueOf(row.get(ModelConstants.ALARM_SEVERITY_PROPERTY).toString()));
        alarm.setStatus(AlarmStatus.valueOf(row.get(ModelConstants.ALARM_STATUS_PROPERTY).toString()));
        alarm.setTenantId(TenantId.fromUUID((UUID) row.get(ModelConstants.TENANT_ID_PROPERTY)));
        Object customerIdObj = row.get(ModelConstants.CUSTOMER_ID_PROPERTY);
        @Nullable CustomerId customerId = customerIdObj != null ? new CustomerId((UUID) customerIdObj) : null;
        alarm.setCustomerId(customerId);
        if (row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES) != null) {
            String propagateRelationTypes = row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES).toString();
            if (!StringUtils.isEmpty(propagateRelationTypes)) {
                alarm.setPropagateRelationTypes(Arrays.asList(propagateRelationTypes.split(",")));
            } else {
                alarm.setPropagateRelationTypes(Collections.emptyList());
            }
        } else {
            alarm.setPropagateRelationTypes(Collections.emptyList());
        }
        UUID entityUuid = (UUID) row.get(ModelConstants.ENTITY_ID_COLUMN);
        EntityId entityId = entityIdMap.get(entityUuid);
        Object originatorNameObj = row.get(ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY);
        String originatorName = originatorNameObj != null ? originatorNameObj.toString() : null;
        return new AlarmData(alarm, originatorName, entityId);
    }

}
