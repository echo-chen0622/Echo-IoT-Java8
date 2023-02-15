package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_SERVICE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class EventEntity<T extends Event> implements BaseEntity<T> {

    public static final Map<String, String> eventColumnMap = new HashMap<>();

    static {
        eventColumnMap.put("createdTime", "ts");
    }

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    @Column(name = EVENT_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    protected UUID tenantId;

    @Column(name = EVENT_ENTITY_ID_PROPERTY, columnDefinition = "uuid")
    protected UUID entityId;

    @Column(name = EVENT_SERVICE_ID_PROPERTY)
    protected String serviceId;

    @Column(name = TS_COLUMN)
    protected long ts;

    public EventEntity(UUID id, UUID tenantId, UUID entityId, String serviceId, long ts) {
        this.id = id;
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.serviceId = serviceId;
        this.ts = ts;
    }

    public EventEntity(Event event) {
        this.id = event.getId().getId();
        this.tenantId = event.getTenantId().getId();
        this.entityId = event.getEntityId();
        this.serviceId = event.getServiceId();
        this.ts = event.getCreatedTime();
    }

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    @Override
    public long getCreatedTime() {
        return ts;
    }

    @Override
    public void setCreatedTime(long createdTime) {
        ts = createdTime;
    }

}
