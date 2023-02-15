package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

public class QueueId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public QueueId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static QueueId fromString(String queueId) {
        return new QueueId(UUID.fromString(queueId));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE;
    }
}
