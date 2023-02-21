package org.echoiot.server.dao.device;

import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;

public interface DeviceCredentialsService {

    DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId);

    DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId);

    DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    void formatCredentials(DeviceCredentials deviceCredentials);

    @Nullable
    JsonNode toCredentialsInfo(DeviceCredentials deviceCredentials);

    void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

}
