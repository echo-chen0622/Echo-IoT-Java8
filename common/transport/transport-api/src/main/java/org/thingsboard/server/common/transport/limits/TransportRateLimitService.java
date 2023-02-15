package org.thingsboard.server.common.transport.limits;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;

import java.net.InetSocketAddress;

public interface TransportRateLimitService {

    EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints);

    void update(TenantProfileUpdateResult update);

    void update(TenantId tenantId);

    void remove(TenantId tenantId);

    void remove(DeviceId deviceId);

    void update(TenantId tenantId, boolean transportEnabled);

    boolean checkAddress(InetSocketAddress address);

    void onAuthSuccess(InetSocketAddress address);

    void onAuthFailure(InetSocketAddress address);

    void invalidateRateLimitsIpTable(long sessionInactivityTimeout);

}
