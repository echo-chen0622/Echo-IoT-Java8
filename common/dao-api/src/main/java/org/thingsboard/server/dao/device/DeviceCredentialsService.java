package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import com.fasterxml.jackson.databind.JsonNode;

public interface DeviceCredentialsService {

    DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId);

    DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId);

    DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    void formatCredentials(DeviceCredentials deviceCredentials);

    JsonNode toCredentialsInfo(DeviceCredentials deviceCredentials);

    void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

}
