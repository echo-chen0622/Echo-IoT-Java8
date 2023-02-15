package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.DeviceRelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.List;

public class EntitiesRelatedDeviceIdAsyncLoader {

    public static ListenableFuture<DeviceId> findDeviceAsync(TbContext ctx, EntityId originator,
                                                             DeviceRelationsQuery deviceRelationsQuery) {
        DeviceService deviceService = ctx.getDeviceService();
        DeviceSearchQuery query = buildQuery(originator, deviceRelationsQuery);

        ListenableFuture<List<Device>> asyncDevices = deviceService.findDevicesByQuery(ctx.getTenantId(), query);

        return Futures.transformAsync(asyncDevices, d -> CollectionUtils.isNotEmpty(d) ? Futures.immediateFuture(d.get(0).getId())
                : Futures.immediateFuture(null), MoreExecutors.directExecutor());
    }

    private static DeviceSearchQuery buildQuery(EntityId originator, DeviceRelationsQuery deviceRelationsQuery) {
        DeviceSearchQuery query = new DeviceSearchQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                deviceRelationsQuery.getDirection(), deviceRelationsQuery.getMaxLevel(), deviceRelationsQuery.isFetchLastLevelOnly());
        query.setParameters(parameters);
        query.setRelationType(deviceRelationsQuery.getRelationType());
        query.setDeviceTypes(deviceRelationsQuery.getDeviceTypes());
        return query;
    }
}
