package org.echoiot.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.echoiot.server.common.data.query.EntityDataQuery;

public class EntityDataCmd extends DataCmd {

    @Getter
    private final EntityDataQuery query;
    @Getter
    private final EntityHistoryCmd historyCmd;
    @Getter
    private final LatestValueCmd latestCmd;
    @Getter
    private final TimeSeriesCmd tsCmd;
    @Getter
    private final AggHistoryCmd aggHistoryCmd;
    @Getter
    private final AggTimeSeriesCmd aggTsCmd;

    public EntityDataCmd(int cmdId, EntityDataQuery query, EntityHistoryCmd historyCmd, LatestValueCmd latestCmd, TimeSeriesCmd tsCmd) {
        this(cmdId, query, historyCmd, latestCmd, tsCmd, null, null);
    }

    @JsonCreator
    public EntityDataCmd(@JsonProperty("cmdId") int cmdId,
                         @JsonProperty("query") EntityDataQuery query,
                         @JsonProperty("historyCmd") EntityHistoryCmd historyCmd,
                         @JsonProperty("latestCmd") LatestValueCmd latestCmd,
                         @JsonProperty("tsCmd") TimeSeriesCmd tsCmd,
                         @JsonProperty("aggHistoryCmd") AggHistoryCmd aggHistoryCmd,
                         @JsonProperty("aggTsCmd") AggTimeSeriesCmd aggTsCmd) {
        super(cmdId);
        this.query = query;
        this.historyCmd = historyCmd;
        this.latestCmd = latestCmd;
        this.tsCmd = tsCmd;
        this.aggHistoryCmd = aggHistoryCmd;
        this.aggTsCmd = aggTsCmd;
    }

    @JsonIgnore
    public boolean hasAnyCmd() {
        return historyCmd != null || latestCmd != null || tsCmd != null || aggHistoryCmd != null || aggTsCmd != null;
    }

}
