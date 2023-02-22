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
