package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

public class EntitiesCustomerIdAsyncLoader {


    public static ListenableFuture<CustomerId> findEntityIdAsync(TbContext ctx, EntityId original) {

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

    private static <T extends HasCustomerId> ListenableFuture<CustomerId> getCustomerAsync(ListenableFuture<T> future) {
        return Futures.transformAsync(future, in -> in != null ? Futures.immediateFuture(in.getCustomerId())
                : Futures.immediateFuture(null), MoreExecutors.directExecutor());
    }
}
