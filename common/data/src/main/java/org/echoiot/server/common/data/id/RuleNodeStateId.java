package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RuleNodeStateId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public RuleNodeStateId(@JsonProperty("id") UUID id) {
        super(id);
    }

    @NotNull
    public static RuleNodeStateId fromString(@NotNull String eventId) {
        return new RuleNodeStateId(UUID.fromString(eventId));
    }
}
