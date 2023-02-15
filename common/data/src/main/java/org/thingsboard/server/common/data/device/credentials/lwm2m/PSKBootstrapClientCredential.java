package org.thingsboard.server.common.data.device.credentials.lwm2m;

public class PSKBootstrapClientCredential extends AbstractLwM2MBootstrapClientCredentialWithKeys {

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.PSK;
    }
}
