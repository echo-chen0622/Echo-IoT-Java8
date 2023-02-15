package org.thingsboard.server.common.data.device.credentials.lwm2m;

public class RPKBootstrapClientCredential extends AbstractLwM2MBootstrapClientCredentialWithKeys {

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.RPK;
    }
}
