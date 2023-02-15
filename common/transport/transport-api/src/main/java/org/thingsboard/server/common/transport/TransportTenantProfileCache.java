package org.thingsboard.server.common.transport;

import com.google.protobuf.ByteString;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.queue.discovery.TenantRoutingInfo;
import org.thingsboard.server.queue.discovery.TenantRoutingInfoService;

import java.util.Set;

public interface TransportTenantProfileCache {

    TenantProfile get(TenantId tenantId);

    TenantProfileUpdateResult put(ByteString profileBody);

    boolean put(TenantId tenantId, TenantProfileId profileId);

    Set<TenantId> remove(TenantProfileId profileId);

}
