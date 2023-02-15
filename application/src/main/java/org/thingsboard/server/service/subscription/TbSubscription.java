package org.thingsboard.server.service.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;
import java.util.function.BiConsumer;

@Data
@AllArgsConstructor
public abstract class TbSubscription<T> {

    private final String serviceId;
    private final String sessionId;
    private final int subscriptionId;
    private final TenantId tenantId;
    private final EntityId entityId;
    private final TbSubscriptionType type;
    private final BiConsumer<String, T> updateConsumer;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TbSubscription that = (TbSubscription) o;
        return subscriptionId == that.subscriptionId &&
                sessionId.equals(that.sessionId) &&
                tenantId.equals(that.tenantId) &&
                entityId.equals(that.entityId) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, subscriptionId, tenantId, entityId, type);
    }
}
