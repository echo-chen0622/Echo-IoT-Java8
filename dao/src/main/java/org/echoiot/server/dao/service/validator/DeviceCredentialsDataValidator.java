package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.device.DeviceCredentialsDao;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.exception.DeviceCredentialsValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeviceCredentialsDataValidator extends DataValidator<DeviceCredentials> {

    @Resource
    private DeviceCredentialsDao deviceCredentialsDao;

    @Resource
    private DeviceService deviceService;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId()) != null) {
            throw new DeviceCredentialsValidationException("Credentials for this device are already specified!");
        }
        if (deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId()) != null) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
    }

    @Nullable
    @Override
    protected DeviceCredentials validateUpdate(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        if (deviceCredentialsDao.findById(tenantId, deviceCredentials.getUuidId()) == null) {
            throw new DeviceCredentialsValidationException("Unable to update non-existent device credentials!");
        }
        DeviceCredentials existingCredentials = deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId());
        if (existingCredentials != null && !existingCredentials.getId().equals(deviceCredentials.getId())) {
            throw new DeviceCredentialsValidationException("Device credentials are already assigned to another device!");
        }
        return existingCredentials;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getDeviceId() == null) {
            throw new DeviceCredentialsValidationException("Device credentials should be assigned to device!");
        }
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DeviceCredentialsValidationException("Device credentials type should be specified!");
        }
        if (StringUtils.isEmpty(deviceCredentials.getCredentialsId())) {
            throw new DeviceCredentialsValidationException("Device credentials id should be specified!");
        }
        Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
        if (device == null) {
            throw new DeviceCredentialsValidationException("Can't assign device credentials to non-existent device!");
        }
    }
}
