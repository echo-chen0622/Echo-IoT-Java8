package org.echoiot.server.service.entitiy.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.device.ClaimDevicesService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.device.claim.ClaimResponse;
import org.echoiot.server.dao.device.claim.ClaimResult;
import org.echoiot.server.dao.device.claim.ReclaimResult;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbDeviceService extends AbstractTbEntityService implements TbDeviceService {

    @NotNull
    private final DeviceService deviceService;
    @NotNull
    private final DeviceCredentialsService deviceCredentialsService;
    @NotNull
    private final ClaimDevicesService claimDevicesService;
    @NotNull
    private final TenantService tenantService;

    @NotNull
    @Override
    public Device save(@NotNull Device device, Device oldDevice, String accessToken, User user) throws Exception {
        @NotNull ActionType actionType = device.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithAccessToken(device, accessToken));
            autoCommit(user, savedDevice.getId());
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    @NotNull
    @Override
    public Device saveDeviceWithCredentials(@NotNull Device device, DeviceCredentials credentials, User user) throws EchoiotException {
        boolean isCreate = device.getId() == null;
        @NotNull ActionType actionType = isCreate ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = device.getTenantId();
        try {
            Device oldDevice = isCreate ? null : deviceService.findDeviceById(tenantId, device.getId());
            Device savedDevice = checkNotNull(deviceService.saveDeviceWithCredentials(device, credentials));
            notificationEntityService.notifyCreateOrUpdateDevice(tenantId, savedDevice.getId(), savedDevice.getCustomerId(),
                    savedDevice, oldDevice, actionType, user);

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), device, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(@NotNull Device device, User user) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            @Nullable List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, deviceId);
            deviceService.deleteDevice(tenantId, deviceId);
            notificationEntityService.notifyDeleteDevice(tenantId, deviceId, device.getCustomerId(), device,
                    relatedEdgeIds, user, deviceId.toString());

            return removeAlarmsByEntityId(tenantId, deviceId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), ActionType.DELETED,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, @NotNull DeviceId deviceId, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, customerId, savedDevice,
                    actionType, user, true, deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType, user,
                    e, deviceId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Device unassignDeviceFromCustomer(@NotNull Device device, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromCustomer(tenantId, deviceId));
            CustomerId customerId = customer.getId();

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, customerId, savedDevice,
                    actionType, user, true, deviceId.toString(), customerId.toString(), customer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @NotNull
    @Override
    public Device assignDeviceToPublicCustomer(TenantId tenantId, @NotNull DeviceId deviceId, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToCustomer(tenantId, deviceId, publicCustomer.getId()));

            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, deviceId, savedDevice.getCustomerId(), savedDevice,
                    actionType, user, false, deviceId.toString(),
                    publicCustomer.getId().toString(), publicCustomer.getName());

            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE), actionType,
                    user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public DeviceCredentials getDeviceCredentialsByDeviceId(@NotNull Device device, User user) throws EchoiotException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials deviceCredentials = checkNotNull(deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId));
            notificationEntityService.logEntityAction(tenantId, deviceId, device, device.getCustomerId(),
                    ActionType.CREDENTIALS_READ, user, deviceId.toString());
            return deviceCredentials;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.CREDENTIALS_READ, user, e, deviceId.toString());
            throw e;
        }
    }

    @Override
    public DeviceCredentials updateDeviceCredentials(@NotNull Device device, DeviceCredentials deviceCredentials, User user) throws EchoiotException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        try {
            DeviceCredentials result = checkNotNull(deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials));
            notificationEntityService.notifyUpdateDeviceCredentials(tenantId, deviceId, device.getCustomerId(), device, result, user);
            return result;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.CREDENTIALS_UPDATED, user, e, deviceCredentials);
            throw e;
        }
    }

    @NotNull
    @Override
    public ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, @NotNull Device device, @NotNull CustomerId customerId, String secretKey, User user) {
        ListenableFuture<ClaimResult> future = claimDevicesService.claimDevice(device, customerId, secretKey);

        return Futures.transform(future, result -> {
            if (result != null && result.getResponse().equals(ClaimResponse.SUCCESS)) {
                notificationEntityService.logEntityAction(tenantId, device.getId(), result.getDevice(), customerId,
                        ActionType.ASSIGNED_TO_CUSTOMER, user, device.getId().toString(), customerId.toString(),
                        customerService.findCustomerById(tenantId, customerId).getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, @NotNull Device device, User user) {
        ListenableFuture<ReclaimResult> future = claimDevicesService.reClaimDevice(tenantId, device);

        return Futures.transform(future, result -> {
            Customer unassignedCustomer = result.getUnassignedCustomer();
            if (unassignedCustomer != null) {
                notificationEntityService.logEntityAction(tenantId, device.getId(), device, device.getCustomerId(),
                        ActionType.UNASSIGNED_FROM_CUSTOMER, user, device.getId().toString(),
                        unassignedCustomer.getId().toString(), unassignedCustomer.getName());
            }
            return result;
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public Device assignDeviceToTenant(@NotNull Device device, @NotNull Tenant newTenant, User user) {
        TenantId tenantId = device.getTenantId();
        TenantId newTenantId = newTenant.getId();
        DeviceId deviceId = device.getId();
        try {
            Tenant tenant = tenantService.findTenantById(tenantId);
            Device assignedDevice = deviceService.assignDeviceToTenant(newTenantId, device);

            notificationEntityService.notifyAssignDeviceToTenant(tenantId, newTenantId, deviceId,
                    assignedDevice.getCustomerId(), assignedDevice, tenant, user, newTenantId.toString(), newTenant.getName());

            return assignedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.ASSIGNED_TO_TENANT, user, e, deviceId.toString());
            throw e;
        }
    }

    @NotNull
    @Override
    public Device assignDeviceToEdge(TenantId tenantId, @NotNull DeviceId deviceId, @NotNull Edge edge, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.assignDeviceToEdge(tenantId, deviceId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, deviceId, savedDevice.getCustomerId(),
                    edgeId, savedDevice, actionType, user, deviceId.toString(), edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.ASSIGNED_TO_EDGE, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Device unassignDeviceFromEdge(@NotNull Device device, @NotNull Edge edge, User user) throws EchoiotException {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        EdgeId edgeId = edge.getId();
        try {
            Device savedDevice = checkNotNull(deviceService.unassignDeviceFromEdge(tenantId, deviceId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, deviceId, device.getCustomerId(),
                    edgeId, device, ActionType.UNASSIGNED_FROM_EDGE, user, deviceId.toString(), edgeId.toString(), edge.getName());
            return savedDevice;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.DEVICE),
                    ActionType.UNASSIGNED_FROM_EDGE, user, e, deviceId.toString(), edgeId.toString());
            throw e;
        }
    }

}
