package org.thingsboard.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.server.common.data.query.AlarmDataQuery;

public class AlarmDataCmd extends DataCmd {

    @Getter
    private final AlarmDataQuery query;

    @JsonCreator
    public AlarmDataCmd(@JsonProperty("cmdId") int cmdId, @JsonProperty("query") AlarmDataQuery query) {
        super(cmdId);
        this.query = query;
    }
}
