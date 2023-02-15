package org.thingsboard.server.service.telemetry.cmd.v1;

import lombok.NoArgsConstructor;
import org.thingsboard.server.service.telemetry.TelemetryFeature;

/**
 * @author Andrew Shvayka
 */
@NoArgsConstructor
public class AttributesSubscriptionCmd extends SubscriptionCmd {

    @Override
    public TelemetryFeature getType() {
        return TelemetryFeature.ATTRIBUTES;
    }

}
