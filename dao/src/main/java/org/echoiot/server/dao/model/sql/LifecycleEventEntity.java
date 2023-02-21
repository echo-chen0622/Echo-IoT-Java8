package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.echoiot.server.common.data.event.LifecycleEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseEntity;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = LC_EVENT_TABLE_NAME)
@NoArgsConstructor
public class LifecycleEventEntity extends EventEntity<LifecycleEvent> implements BaseEntity<LifecycleEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_SUCCESS_COLUMN_NAME)
    private boolean success;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public LifecycleEventEntity(@NotNull LifecycleEvent event) {
        super(event);
        this.eventType = event.getLcEventType();
        this.success = event.isSuccess();
        this.error = event.getError();
    }

    @Override
    public LifecycleEvent toData() {
        return LifecycleEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .lcEventType(eventType)
                .success(success)
                .error(error)
                .build();
    }

}
