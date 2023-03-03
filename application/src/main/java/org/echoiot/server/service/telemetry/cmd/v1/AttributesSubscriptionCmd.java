package org.echoiot.server.service.telemetry.cmd.v1;

import lombok.NoArgsConstructor;
import org.echoiot.server.service.telemetry.TelemetryFeature;

/**
 * @author Echo
 */
@NoArgsConstructor
public class AttributesSubscriptionCmd extends SubscriptionCmd {

    @Override
    public TelemetryFeature getType() {
        return TelemetryFeature.ATTRIBUTES;
    }

}
