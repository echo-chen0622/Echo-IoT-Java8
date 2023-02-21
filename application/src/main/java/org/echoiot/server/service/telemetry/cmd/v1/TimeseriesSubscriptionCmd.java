package org.echoiot.server.service.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.echoiot.server.service.telemetry.TelemetryFeature;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    @Override
    public TelemetryFeature getType() {
        return TelemetryFeature.TIMESERIES;
    }
}
