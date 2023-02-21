package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.device.DeviceService;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Slf4j
public class DevicesEdgeEventFetcher extends BasePageableEdgeEventFetcher<Device> {

    @NotNull
    private final DeviceService deviceService;

    @Override
    PageData<Device> fetchPageData(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) {
        return deviceService.findDevicesByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull Device device) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE,
                                            EdgeEventActionType.ADDED, device.getId(), null);
    }
}
