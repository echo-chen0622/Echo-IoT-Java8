package org.echoiot.server.dao.relation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;

@RequiredArgsConstructor
public class EntityRelationEvent {
    @Getter
    private final EntityId from;
    @Getter
    private final EntityId to;
    @Getter
    private final String type;
    @Getter
    private final RelationTypeGroup typeGroup;

    public static EntityRelationEvent from(EntityRelation relation) {
        return new EntityRelationEvent(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }
}
