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

@AllArgsConstructor
@Slf4j
public class DevicesEdgeEventFetcher extends BasePageableEdgeEventFetcher<Device> {

    private final DeviceService deviceService;

    @Override
    PageData<Device> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return deviceService.findDevicesByTenantIdAndEdgeId(tenantId, edge.getId(), pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, Device device) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE,
                                            EdgeEventActionType.ADDED, device.getId(), null);
    }
}
