package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;

public class EntitiesAlarmOriginatorIdAsyncLoader {

    public static ListenableFuture<EntityId> findEntityIdAsync(TbContext ctx, EntityId original) {

        switch (original.getEntityType()) {
            case ALARM:
                return getAlarmOriginatorAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) original));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
        }
    }

    private static ListenableFuture<EntityId> getAlarmOriginatorAsync(ListenableFuture<Alarm> future) {
        return Futures.transformAsync(future, in -> {
            return in != null ? Futures.immediateFuture(in.getOriginator())
                    : Futures.immediateFuture(null);
        }, MoreExecutors.directExecutor());
    }
}
