package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.Map;
import java.util.function.BiConsumer;

public class TbTimeseriesSubscription extends TbSubscription<TelemetrySubscriptionUpdate> {

    @Getter
    private final boolean allKeys;
    @Getter
    private final Map<String, Long> keyStates;
    @Getter
    private final long startTime;
    @Getter
    private final long endTime;
    @Getter
    private final boolean latestValues;

    @Builder
    public TbTimeseriesSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                    BiConsumer<String, TelemetrySubscriptionUpdate> updateConsumer,
                                    boolean allKeys, Map<String, Long> keyStates, long startTime, long endTime, boolean latestValues) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.TIMESERIES, updateConsumer);
        this.allKeys = allKeys;
        this.keyStates = keyStates;
        this.startTime = startTime;
        this.endTime = endTime;
        this.latestValues = latestValues;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
