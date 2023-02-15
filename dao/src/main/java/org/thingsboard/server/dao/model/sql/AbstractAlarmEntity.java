package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ACK_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CLEAR_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_END_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_RELATION_TYPES;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_SEVERITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_START_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAlarmEntity<T extends Alarm> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = ALARM_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ALARM_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ALARM_ORIGINATOR_ID_PROPERTY)
    private UUID originatorId;

    @Column(name = ALARM_ORIGINATOR_TYPE_PROPERTY)
    private EntityType originatorType;

    @Column(name = ALARM_TYPE_PROPERTY)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_SEVERITY_PROPERTY)
    private AlarmSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_STATUS_PROPERTY)
    private AlarmStatus status;

    @Column(name = ALARM_START_TS_PROPERTY)
    private Long startTs;

    @Column(name = ALARM_END_TS_PROPERTY)
    private Long endTs;

    @Column(name = ALARM_ACK_TS_PROPERTY)
    private Long ackTs;

    @Column(name = ALARM_CLEAR_TS_PROPERTY)
    private Long clearTs;

    @Type(type = "json")
    @Column(name = ModelConstants.ASSET_ADDITIONAL_INFO_PROPERTY)
    private JsonNode details;

    @Column(name = ALARM_PROPAGATE_PROPERTY)
    private Boolean propagate;

    @Column(name = ALARM_PROPAGATE_TO_OWNER_PROPERTY)
    private Boolean propagateToOwner;

    @Column(name = ALARM_PROPAGATE_TO_TENANT_PROPERTY)
    private Boolean propagateToTenant;

    @Column(name = ALARM_PROPAGATE_RELATION_TYPES)
    private String propagateRelationTypes;

    public AbstractAlarmEntity() {
        super();
    }

    public AbstractAlarmEntity(Alarm alarm) {
        if (alarm.getId() != null) {
            this.setUuid(alarm.getUuidId());
        }
        this.setCreatedTime(alarm.getCreatedTime());
        if (alarm.getTenantId() != null) {
            this.tenantId = alarm.getTenantId().getId();
        }
        if (alarm.getCustomerId() != null) {
            this.customerId = alarm.getCustomerId().getId();
        }
        this.type = alarm.getType();
        this.originatorId = alarm.getOriginator().getId();
        this.originatorType = alarm.getOriginator().getEntityType();
        this.type = alarm.getType();
        this.severity = alarm.getSeverity();
        this.status = alarm.getStatus();
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.details = alarm.getDetails();
        if (!CollectionUtils.isEmpty(alarm.getPropagateRelationTypes())) {
            this.propagateRelationTypes = String.join(",", alarm.getPropagateRelationTypes());
        } else {
            this.propagateRelationTypes = null;
        }
    }

    public AbstractAlarmEntity(AlarmEntity alarmEntity) {
        this.setId(alarmEntity.getId());
        this.setCreatedTime(alarmEntity.getCreatedTime());
        this.tenantId = alarmEntity.getTenantId();
        this.customerId = alarmEntity.getCustomerId();
        this.type = alarmEntity.getType();
        this.originatorId = alarmEntity.getOriginatorId();
        this.originatorType = alarmEntity.getOriginatorType();
        this.type = alarmEntity.getType();
        this.severity = alarmEntity.getSeverity();
        this.status = alarmEntity.getStatus();
        this.propagate = alarmEntity.getPropagate();
        this.propagateToOwner = alarmEntity.getPropagateToOwner();
        this.propagateToTenant = alarmEntity.getPropagateToTenant();
        this.startTs = alarmEntity.getStartTs();
        this.endTs = alarmEntity.getEndTs();
        this.ackTs = alarmEntity.getAckTs();
        this.clearTs = alarmEntity.getClearTs();
        this.details = alarmEntity.getDetails();
        this.propagateRelationTypes = alarmEntity.getPropagateRelationTypes();
    }

    protected Alarm toAlarm() {
        Alarm alarm = new Alarm(new AlarmId(id));
        alarm.setCreatedTime(createdTime);
        if (tenantId != null) {
            alarm.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            alarm.setCustomerId(new CustomerId(customerId));
        }
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        alarm.setType(type);
        alarm.setSeverity(severity);
        alarm.setStatus(status);
        alarm.setPropagate(propagate);
        alarm.setPropagateToOwner(propagateToOwner);
        alarm.setPropagateToTenant(propagateToTenant);
        alarm.setStartTs(startTs);
        alarm.setEndTs(endTs);
        alarm.setAckTs(ackTs);
        alarm.setClearTs(clearTs);
        alarm.setDetails(details);
        if (!StringUtils.isEmpty(propagateRelationTypes)) {
            alarm.setPropagateRelationTypes(Arrays.asList(propagateRelationTypes.split(",")));
        } else {
            alarm.setPropagateRelationTypes(Collections.emptyList());
        }
        return alarm;
    }
}
