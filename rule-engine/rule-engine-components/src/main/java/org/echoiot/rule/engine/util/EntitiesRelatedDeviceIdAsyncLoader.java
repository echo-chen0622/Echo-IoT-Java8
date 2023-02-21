package org.echoiot.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections.CollectionUtils;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.data.DeviceRelationsQuery;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.device.DeviceSearchQuery;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.RelationsSearchParameters;
import org.echoiot.server.dao.device.DeviceService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntitiesRelatedDeviceIdAsyncLoader {

    @NotNull
    public static ListenableFuture<DeviceId> findDeviceAsync(@NotNull TbContext ctx, @NotNull EntityId originator,
                                                             @NotNull DeviceRelationsQuery deviceRelationsQuery) {
        DeviceService deviceService = ctx.getDeviceService();
        @NotNull DeviceSearchQuery query = buildQuery(originator, deviceRelationsQuery);

        ListenableFuture<List<Device>> asyncDevices = deviceService.findDevicesByQuery(ctx.getTenantId(), query);

        return Futures.transformAsync(asyncDevices, d -> CollectionUtils.isNotEmpty(d) ? Futures.immediateFuture(d.get(0).getId())
                : Futures.immediateFuture(null), MoreExecutors.directExecutor());
    }

    @NotNull
    private static DeviceSearchQuery buildQuery(@NotNull EntityId originator, @NotNull DeviceRelationsQuery deviceRelationsQuery) {
        @NotNull DeviceSearchQuery query = new DeviceSearchQuery();
        @NotNull RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                                                                                      deviceRelationsQuery.getDirection(), deviceRelationsQuery.getMaxLevel(), deviceRelationsQuery.isFetchLastLevelOnly());
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }
}
