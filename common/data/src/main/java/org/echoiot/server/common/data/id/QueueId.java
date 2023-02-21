package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class QueueId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public QueueId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static QueueId fromString(@NotNull String queueId) {
        return new QueueId(UUID.fromString(queueId));
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE;
    }
}
