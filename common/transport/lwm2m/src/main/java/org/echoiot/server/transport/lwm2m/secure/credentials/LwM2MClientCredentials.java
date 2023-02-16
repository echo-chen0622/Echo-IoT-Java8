package org.echoiot.server.transport.lwm2m.secure.credentials;

import lombok.Data;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.echoiot.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;

@Data
public class LwM2MClientCredentials {
    private LwM2MClientCredential client;
    private LwM2MBootstrapConfig bootstrap;
}
