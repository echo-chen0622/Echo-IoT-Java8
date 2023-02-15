package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ACTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_BODY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_EDGE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_UID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

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

    public EdgeEventEntity(EdgeEvent edgeEvent) {
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

    @Override
    public EdgeEvent toData() {
        EdgeEvent edgeEvent = new EdgeEvent(new EdgeEventId(this.getUuid()));
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

    private static long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
