package org.echoiot.server.common.data.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.EventId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Event extends BaseData<EventId> {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected final TenantId tenantId;
    protected final UUID entityId;
    protected final String serviceId;

    public Event(@Nullable TenantId tenantId, UUID entityId, String serviceId, @Nullable UUID id, long ts) {
        super();
        if (id != null) {
            this.id = new EventId(id);
        }
        this.tenantId = tenantId != null ? tenantId : TenantId.SYS_TENANT_ID;
        this.entityId = entityId;
        this.serviceId = serviceId;
        this.createdTime = ts;
    }

    public abstract EventType getType();

    public EventInfo toInfo(@NotNull EntityType entityType) {
        @NotNull EventInfo eventInfo = new EventInfo();
        eventInfo.setTenantId(tenantId);
        eventInfo.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        eventInfo.setType(getType().getOldName());
        eventInfo.setId(id);
        eventInfo.setUid(id.toString());
        eventInfo.setCreatedTime(createdTime);
        eventInfo.setBody(OBJECT_MAPPER.createObjectNode().put("server", getServiceId()));
        return eventInfo;
    }

    protected static void putNotNull(@NotNull ObjectNode json, String key, @Nullable String value) {
        if (value != null) {
            json.put(key, value);
        }
    }
}
