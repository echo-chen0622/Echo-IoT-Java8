package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EdgeEventId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public EdgeEventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static EdgeEventId fromString(@NotNull String edgeEventId) {
        return new EdgeEventId(UUID.fromString(edgeEventId));
    }
}
