package org.thingsboard.server.service.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.telemetry.TelemetryFeature;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TimeseriesSubscriptionCmd extends SubscriptionCmd {

    private long startTs;
    private long timeWindow;
    private long interval;
    private int limit;
    private String agg;

    @Override
    public TelemetryFeature getType() {
        return TelemetryFeature.TIMESERIES;
    }
}
