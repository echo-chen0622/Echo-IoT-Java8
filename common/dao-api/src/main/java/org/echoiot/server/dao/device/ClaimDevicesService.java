package org.echoiot.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.device.claim.ClaimResult;
import org.echoiot.server.dao.device.claim.ReclaimResult;

public interface ClaimDevicesService {

    ListenableFuture<Void> registerClaimingInfo(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs);

    ListenableFuture<ClaimResult> claimDevice(Device device, CustomerId customerId, String secretKey);

    ListenableFuture<ReclaimResult> reClaimDevice(TenantId tenantId, Device device);

}
