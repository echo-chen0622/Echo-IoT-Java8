package org.echoiot.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.echoiot.server.common.data.query.EntityCountQuery;

public class EntityCountCmd extends DataCmd {

    @Getter
    private final EntityCountQuery query;

    @JsonCreator
    public EntityCountCmd(@JsonProperty("cmdId") int cmdId,
                          @JsonProperty("query") EntityCountQuery query) {
        super(cmdId);
        this.query = query;
    }
}
