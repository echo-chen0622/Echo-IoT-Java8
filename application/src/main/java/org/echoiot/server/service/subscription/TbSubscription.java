package org.echoiot.server.service.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;

@Data
@AllArgsConstructor
public abstract class TbSubscription<T> {

    @NotNull
    private final String serviceId;
    @NotNull
    private final String sessionId;
    private final int subscriptionId;
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final EntityId entityId;
    @NotNull
    private final TbSubscriptionType type;
    @NotNull
    private final BiConsumer<String, T> updateConsumer;

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @NotNull TbSubscription that = (TbSubscription) o;
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
