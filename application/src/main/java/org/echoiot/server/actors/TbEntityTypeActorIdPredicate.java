package org.echoiot.server.actors;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class TbEntityTypeActorIdPredicate implements Predicate<TbActorId> {

    @NotNull
    private final EntityType entityType;

    @Override
    public boolean test(TbActorId actorId) {
        return actorId instanceof TbEntityActorId && testEntityId(((TbEntityActorId) actorId).getEntityId());
    }

    protected boolean testEntityId(@NotNull EntityId entityId) {
        return entityId.getEntityType().equals(entityType);
    }
}
