package org.echoiot.server.dao.model.sql;

import lombok.Data;
import org.echoiot.server.common.data.alarm.EntityAlarm;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.ToData;

import javax.persistence.*;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = ENTITY_ALARM_COLUMN_FAMILY_NAME)
@IdClass(EntityAlarmCompositeKey.class)
public final class EntityAlarmEntity implements ToData<EntityAlarm> {

    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;

    @Id
    @Column(name = "alarm_id", columnDefinition = "uuid")
    private UUID alarmId;

    @Column(name = CREATED_TIME_PROPERTY)
    private long createdTime;

    @Column(name = "alarm_type")
    private String alarmType;

    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    public EntityAlarmEntity() {
        super();
    }

    public EntityAlarmEntity(EntityAlarm entityAlarm) {
        tenantId = entityAlarm.getTenantId().getId();
        entityId = entityAlarm.getEntityId().getId();
        entityType = entityAlarm.getEntityId().getEntityType().name();
        alarmId = entityAlarm.getAlarmId().getId();
        alarmType = entityAlarm.getAlarmType();
        createdTime = entityAlarm.getCreatedTime();
        if (entityAlarm.getCustomerId() != null) {
            customerId = entityAlarm.getCustomerId().getId();
        }
    }

    @Override
    public EntityAlarm toData() {
        EntityAlarm result = new EntityAlarm();
        result.setTenantId(TenantId.fromUUID(tenantId));
        result.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        result.setAlarmId(new AlarmId(alarmId));
        result.setAlarmType(alarmType);
        result.setCreatedTime(createdTime);
        if (customerId != null) {
            result.setCustomerId(new CustomerId(customerId));
        }
        return result;
    }

}
