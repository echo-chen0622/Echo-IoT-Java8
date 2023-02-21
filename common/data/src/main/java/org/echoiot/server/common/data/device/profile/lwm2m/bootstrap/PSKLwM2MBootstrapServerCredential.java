package org.echoiot.server.common.data.device.profile.lwm2m.bootstrap;

import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.jetbrains.annotations.NotNull;

public class PSKLwM2MBootstrapServerCredential extends AbstractLwM2MBootstrapServerCredential {

    private static final long serialVersionUID = -1639587501559199887L;

    @NotNull
    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.PSK;
    }
}
