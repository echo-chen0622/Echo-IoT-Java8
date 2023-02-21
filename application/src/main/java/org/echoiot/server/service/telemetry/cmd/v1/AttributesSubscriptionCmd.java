package org.echoiot.server.service.telemetry.cmd.v1;

import lombok.NoArgsConstructor;
import org.echoiot.server.service.telemetry.TelemetryFeature;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
public class AttributesSubscriptionCmd extends SubscriptionCmd {

    @NotNull
    @Override
    public TelemetryFeature getType() {
        return TelemetryFeature.ATTRIBUTES;
    }

}
