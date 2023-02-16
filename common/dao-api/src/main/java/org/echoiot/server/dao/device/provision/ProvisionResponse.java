package org.echoiot.server.dao.device.provision;

import lombok.Data;
import org.echoiot.server.common.data.security.DeviceCredentials;

@Data
public class ProvisionResponse {
    private final DeviceCredentials deviceCredentials;
    private final ProvisionResponseStatus responseStatus;
}
