package org.echoiot.server.service.telemetry.sub;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.service.telemetry.TelemetryFeature;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * @author Andrew Shvayka
 */
@AllArgsConstructor
public class SubscriptionState {

    @Getter private final String wsSessionId;
    @Getter private final int subscriptionId;
    @Getter private final TenantId tenantId;
    @Getter private final EntityId entityId;
    @Getter private final TelemetryFeature type;
    @Getter private final boolean allKeys;
    @Getter private final Map<String, Long> keyStates;
    @Getter private final String scope;

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionState that = (SubscriptionState) o;

        if (subscriptionId != that.subscriptionId) return false;
        if (!Objects.equals(wsSessionId, that.wsSessionId)) return false;
        if (!Objects.equals(entityId, that.entityId)) return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = wsSessionId != null ? wsSessionId.hashCode() : 0;
        result = 31 * result + subscriptionId;
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SubscriptionState{" +
                "type=" + type +
                ", entityId=" + entityId +
                ", subscriptionId=" + subscriptionId +
                ", wsSessionId='" + wsSessionId + '\'' +
                '}';
    }
}
