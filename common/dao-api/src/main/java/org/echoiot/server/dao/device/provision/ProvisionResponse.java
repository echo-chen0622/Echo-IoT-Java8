package org.echoiot.server.dao.device.provision;

import lombok.Data;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.jetbrains.annotations.NotNull;

@Data
public class ProvisionResponse {
    @NotNull
    private final DeviceCredentials deviceCredentials;
    @NotNull
    private final ProvisionResponseStatus responseStatus;
}
