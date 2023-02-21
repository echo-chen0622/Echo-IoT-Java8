package org.echoiot.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.EntityFieldsData;
import org.echoiot.server.common.data.id.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class EntitiesFieldsAsyncLoader {

    @NotNull
    public static ListenableFuture<EntityFieldsData> findAsync(@NotNull TbContext ctx, @NotNull EntityId original) {
        switch (original.getEntityType()) {
            case TENANT:
                return getAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), (TenantId) original),
                        EntityFieldsData::new);
            case CUSTOMER:
                return getAsync(ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), (CustomerId) original),
                        EntityFieldsData::new);
            case USER:
                return getAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) original),
                        EntityFieldsData::new);
            case ASSET:
                return getAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) original),
                        EntityFieldsData::new);
            case DEVICE:
                return getAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), (DeviceId) original),
                        EntityFieldsData::new);
            case ALARM:
                return getAsync(ctx.getAlarmService().findAlarmByIdAsync(ctx.getTenantId(), (AlarmId) original),
                        EntityFieldsData::new);
            case RULE_CHAIN:
                return getAsync(ctx.getRuleChainService().findRuleChainByIdAsync(ctx.getTenantId(), (RuleChainId) original),
                        EntityFieldsData::new);
            case ENTITY_VIEW:
                return getAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) original),
                        EntityFieldsData::new);
            default:
                return Futures.immediateFailedFuture(new TbNodeException("Unexpected original EntityType " + original.getEntityType()));
        }
    }

    @NotNull
    private static <T extends BaseData> ListenableFuture<EntityFieldsData> getAsync(
            @NotNull ListenableFuture<T> future, @NotNull Function<T, EntityFieldsData> converter) {
        return Futures.transformAsync(future, in -> in != null ?
                Futures.immediateFuture(converter.apply(in))
                : Futures.immediateFailedFuture(new RuntimeException("Entity not found!")), MoreExecutors.directExecutor());
    }
}
