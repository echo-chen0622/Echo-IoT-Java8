package org.thingsboard.server.common.data.device.credentials.lwm2m;

public class NoSecBootstrapClientCredential implements LwM2MBootstrapClientCredential {

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.NO_SEC;
    }
}
