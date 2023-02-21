package org.echoiot.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.HasCustomerId;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UserId;
import org.jetbrains.annotations.NotNull;

public class EntitiesCustomerIdAsyncLoader {


    @NotNull
    public static ListenableFuture<CustomerId> findEntityIdAsync(@NotNull TbContext ctx, @NotNull EntityId original) {

        switch (original.getEntityType()) {
            case CUSTOMER:
                return Futures.immediateFuture((CustomerId) original);
            case USER:
                return getCustomerAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) original));
            case ASSET:
                return getCustomerAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) original));
            case DEVICE:
                return getCustomerAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), (DeviceId) original));
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
        }
    }

    @NotNull
    private static <T extends HasCustomerId> ListenableFuture<CustomerId> getCustomerAsync(@NotNull ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> in != null ? Futures.immediateFuture(in.getCustomerId())
                : Futures.immediateFuture(null), MoreExecutors.directExecutor());
    }
}
