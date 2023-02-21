package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.EdgeEventId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = EDGE_EVENT_COLUMN_FAMILY_NAME)
@NoArgsConstructor
public class EdgeEventEntity extends BaseSqlEntity<EdgeEvent> implements BaseEntity<EdgeEvent> {

    @Column(name = EDGE_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = EDGE_EVENT_EDGE_ID_PROPERTY)
    private UUID edgeId;

    @Column(name = EDGE_EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_TYPE_PROPERTY)
    private EdgeEventType edgeEventType;

    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_ACTION_PROPERTY)
    private EdgeEventActionType edgeEventAction;

    @Type(type = "json")
    @Column(name = EDGE_EVENT_BODY_PROPERTY)
    private JsonNode entityBody;

    @Column(name = EDGE_EVENT_UID_PROPERTY)
    private String edgeEventUid;

    @Column(name = TS_COLUMN)
    private long ts;

    public EdgeEventEntity(@NotNull EdgeEvent edgeEvent) {
        if (edgeEvent.getId() != null) {
            this.setUuid(edgeEvent.getId().getId());
            this.ts = getTs(edgeEvent.getId().getId());
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.setCreatedTime(edgeEvent.getCreatedTime());
        if (edgeEvent.getTenantId() != null) {
            this.tenantId = edgeEvent.getTenantId().getId();
        }
        if (edgeEvent.getEdgeId() != null) {
            this.edgeId = edgeEvent.getEdgeId().getId();
        }
        if (edgeEvent.getEntityId() != null) {
            this.entityId = edgeEvent.getEntityId();
        }
        this.edgeEventType = edgeEvent.getType();
        this.edgeEventAction = edgeEvent.getAction();
        this.entityBody = edgeEvent.getBody();
        this.edgeEventUid = edgeEvent.getUid();
    }

    @NotNull
    @Override
    public EdgeEvent toData() {
        @NotNull EdgeEvent edgeEvent = new EdgeEvent(new EdgeEventId(this.getUuid()));
        edgeEvent.setCreatedTime(createdTime);
        edgeEvent.setTenantId(TenantId.fromUUID(tenantId));
        edgeEvent.setEdgeId(new EdgeId(edgeId));
        if (entityId != null) {
            edgeEvent.setEntityId(entityId);
        }
        edgeEvent.setType(edgeEventType);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(entityBody);
        edgeEvent.setUid(edgeEventUid);
        return edgeEvent;
    }

    private static long getTs(@NotNull UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
