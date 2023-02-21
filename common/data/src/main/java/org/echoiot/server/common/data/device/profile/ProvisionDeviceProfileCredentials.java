package org.echoiot.server.common.data.device.profile;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class ProvisionDeviceProfileCredentials {
    @NotNull
    private final String provisionDeviceKey;
    @NotNull
    private final String provisionDeviceSecret;
}
