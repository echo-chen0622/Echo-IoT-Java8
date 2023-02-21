package org.echoiot.server.common.data.device.credentials.lwm2m;

import org.jetbrains.annotations.NotNull;

public class PSKBootstrapClientCredential extends AbstractLwM2MBootstrapClientCredentialWithKeys {

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.PSK;
    }
}
