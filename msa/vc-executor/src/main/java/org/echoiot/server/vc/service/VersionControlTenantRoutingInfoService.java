package org.echoiot.server.vc.service;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.queue.discovery.TenantRoutingInfo;
import org.echoiot.server.queue.discovery.TenantRoutingInfoService;
import org.springframework.stereotype.Service;

@Service
public class VersionControlTenantRoutingInfoService implements TenantRoutingInfoService {
    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        //This dummy implementation is ok since Version Control service does not produce any rule engine messages.
        return new TenantRoutingInfo(tenantId, false);
    }
}
