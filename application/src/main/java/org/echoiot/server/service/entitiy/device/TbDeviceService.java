package org.echoiot.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.device.claim.ClaimResult;
import org.echoiot.server.dao.device.claim.ReclaimResult;

public interface TbDeviceService {

    Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception;

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    ListenableFuture<Void> delete(Device device, User user);

    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws ThingsboardException;

    Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws ThingsboardException;

    Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws ThingsboardException;

    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException;

    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user);

    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user);

    Device assignDeviceToTenant(Device device, Tenant newTenant, User user);

    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws ThingsboardException;

    Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws ThingsboardException;
}
