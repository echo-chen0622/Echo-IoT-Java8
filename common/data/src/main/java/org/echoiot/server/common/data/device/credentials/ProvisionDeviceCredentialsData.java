package org.echoiot.server.common.data.device.credentials;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class ProvisionDeviceCredentialsData {
    @NotNull
    private final String token;
    @NotNull
    private final String clientId;
    @NotNull
    private final String username;
    @NotNull
    private final String password;
    @NotNull
    private final String x509CertHash;
}
