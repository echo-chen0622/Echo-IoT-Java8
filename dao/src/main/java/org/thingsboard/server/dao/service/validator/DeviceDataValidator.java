package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class DeviceDataValidator extends AbstractHasOtaPackageValidator<Device> {

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, Device device) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxDevices = profileConfiguration.getMaxDevices();
        validateNumberOfEntitiesPerTenant(tenantId, deviceDao, maxDevices, EntityType.DEVICE);
    }

    @Override
    protected Device validateUpdate(TenantId tenantId, Device device) {
        Device old = deviceDao.findById(device.getTenantId(), device.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing device!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Device device) {
        if (StringUtils.isEmpty(device.getName()) || device.getName().trim().length() == 0) {
            throw new DataValidationException("Device name should be specified!");
        }
        if (device.getTenantId() == null) {
            throw new DataValidationException("Device should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(device.getTenantId())) {
                throw new DataValidationException("Device is referencing to non-existent tenant!");
            }
        }
        if (device.getCustomerId() == null) {
            device.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(device.getTenantId(), device.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign device to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(device.getTenantId().getId())) {
                throw new DataValidationException("Can't assign device to customer from different tenant!");
            }
        }
        Optional.ofNullable(device.getDeviceData())
                .flatMap(deviceData -> Optional.ofNullable(deviceData.getTransportConfiguration()))
                .ifPresent(DeviceTransportConfiguration::validate);

        validateOtaPackage(tenantId, device, device.getDeviceProfileId());
    }
}
