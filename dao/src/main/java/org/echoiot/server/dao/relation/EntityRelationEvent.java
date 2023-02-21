package org.echoiot.server.dao.relation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class EntityRelationEvent {
    @NotNull
    @Getter
    private final EntityId from;
    @NotNull
    @Getter
    private final EntityId to;
    @NotNull
    @Getter
    private final String type;
    @NotNull
    @Getter
    private final RelationTypeGroup typeGroup;

    @NotNull
    public static EntityRelationEvent from(@NotNull EntityRelation relation) {
        return new EntityRelationEvent(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }
}
