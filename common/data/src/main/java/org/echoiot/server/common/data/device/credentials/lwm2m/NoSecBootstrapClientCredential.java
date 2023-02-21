package org.echoiot.server.common.data.device.credentials.lwm2m;

import org.jetbrains.annotations.NotNull;

public class NoSecBootstrapClientCredential implements LwM2MBootstrapClientCredential {

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
