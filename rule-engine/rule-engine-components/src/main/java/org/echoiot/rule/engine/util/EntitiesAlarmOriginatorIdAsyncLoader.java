package org.echoiot.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EntitiesAlarmOriginatorIdAsyncLoader {

    @NotNull
    public static ListenableFuture<EntityId> findEntityIdAsync(@NotNull TbContext ctx, @NotNull EntityId original) {

        if (Objects.requireNonNull(original.getEntityType()) == EntityType.ALARM) {
            return getAlarmOriginatorAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) original));
        }
        return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
    }

    @NotNull
    private static ListenableFuture<EntityId> getAlarmOriginatorAsync(@NotNull ListenableFuture<Alarm> future) {
        return Futures.transformAsync(future, in -> {
            return in != null ? Futures.immediateFuture(in.getOriginator())
                    : Futures.immediateFuture(null);
        }, MoreExecutors.directExecutor());
    }
}
