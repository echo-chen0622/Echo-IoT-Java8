package org.thingsboard.server.dao.device.provision;

import lombok.Data;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@Data
public class ProvisionResponse {
    private final DeviceCredentials deviceCredentials;
    private final ProvisionResponseStatus responseStatus;
}
