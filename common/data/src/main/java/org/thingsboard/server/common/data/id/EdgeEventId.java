package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EdgeEventId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EdgeEventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static EdgeEventId fromString(String edgeEventId) {
        return new EdgeEventId(UUID.fromString(edgeEventId));
    }
}
