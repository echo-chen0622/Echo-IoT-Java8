package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Slf4j
public class DeviceProfilesEdgeEventFetcher extends BasePageableEdgeEventFetcher<DeviceProfile> {

    @NotNull
    private final DeviceProfileService deviceProfileService;

    @Override
    PageData<DeviceProfile> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return deviceProfileService.findDeviceProfiles(tenantId, pageLink);
    }

    @NotNull
    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, @NotNull Edge edge, @NotNull DeviceProfile deviceProfile) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE,
                                            EdgeEventActionType.ADDED, deviceProfile.getId(), null);
    }
}
