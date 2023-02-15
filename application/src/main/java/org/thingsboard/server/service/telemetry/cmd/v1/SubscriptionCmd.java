package org.thingsboard.server.service.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.telemetry.TelemetryFeature;

@NoArgsConstructor
@AllArgsConstructor
@Data
public abstract class SubscriptionCmd implements TelemetryPluginCmd {

    private int cmdId;
    private String entityType;
    private String entityId;
    private String keys;
    private String scope;
    private boolean unsubscribe;

    public abstract TelemetryFeature getType();

    @Override
    public String toString() {
        return "SubscriptionCmd [entityType=" + entityType  + ", entityId=" + entityId + ", tags=" + keys + ", unsubscribe=" + unsubscribe + "]";
    }

}
