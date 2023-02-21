package org.echoiot.server.common.data.device.credentials.lwm2m;

import org.jetbrains.annotations.NotNull;

public class NoSecClientCredential extends AbstractLwM2MClientCredential {

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityConfigClientMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
