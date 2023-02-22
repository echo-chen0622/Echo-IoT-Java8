package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.echoiot.server.common.data.event.RuleNodeDebugEvent;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_NODE_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleNodeDebugEventEntity extends EventEntity<RuleNodeDebugEvent> implements BaseEntity<RuleNodeDebugEvent> {

    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    @Column(name = EVENT_DATA_TYPE_COLUMN_NAME)
    private String dataType;
    @Column(name = EVENT_RELATION_TYPE_COLUMN_NAME)
    private String relationType;
    @Column(name = EVENT_DATA_COLUMN_NAME)
    private String data;
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public RuleNodeDebugEventEntity(RuleNodeDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.dataType = event.getDataType();
        this.relationType = event.getRelationType();
        this.data = event.getData();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    @Override
    public RuleNodeDebugEvent toData() {
        var builder = RuleNodeDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .msgId(msgId)
                .msgType(msgType)
                .dataType(dataType)
                .relationType(relationType)
                .data(data)
                .metadata(metadata)
                .error(error);
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
