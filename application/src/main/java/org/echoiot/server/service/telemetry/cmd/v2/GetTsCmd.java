package org.echoiot.server.service.telemetry.cmd.v2;

import org.echoiot.server.common.data.kv.Aggregation;

import java.util.List;

public interface GetTsCmd {

    long getStartTs();

    long getEndTs();

    List<String> getKeys();

    long getInterval();

    int getLimit();

    Aggregation getAgg();

    boolean isFetchLatestPreviousPoint();

}
