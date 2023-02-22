package org.echoiot.server.actors;

import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class TbStringActorId implements TbActorId {

    private final String id;

    public TbStringActorId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbStringActorId that = (TbStringActorId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Nullable
    @Override
    public EntityType getEntityType() {
        return null;
    }
}
