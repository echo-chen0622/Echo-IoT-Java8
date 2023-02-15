package org.thingsboard.server.service.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetHistoryCmd implements TelemetryPluginCmd {

    private int cmdId;
    private String entityType;
    private String entityId;
    private String keys;
    private long startTs;
    private long endTs;
    private long interval;
    private int limit;
    private String agg;

}
