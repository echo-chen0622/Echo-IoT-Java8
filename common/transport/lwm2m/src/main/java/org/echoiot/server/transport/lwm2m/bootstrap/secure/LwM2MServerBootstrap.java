package org.echoiot.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Data
public class LwM2MServerBootstrap {

    String clientPublicKeyOrId = "";
    String clientSecretKey = "";
    String serverPublicKey = "";
    Integer clientHoldOffTime = 1;
    Integer bootstrapServerAccountTimeout = 0;

    @NotNull
    String host = "0.0.0.0";
    Integer port = 0;
    @NotNull
    String securityHost = "0.0.0.0";
    Integer securityPort = 0;

    SecurityMode securityMode = SecurityMode.NO_SEC;

    Integer serverId = 123;
    boolean bootstrapServerIs = false;

    public LwM2MServerBootstrap() {
    }

    public LwM2MServerBootstrap(@NotNull LwM2MServerBootstrap bootstrapFromCredential, @NotNull LwM2MServerBootstrap profileServerBootstrap) {
        this.clientPublicKeyOrId = bootstrapFromCredential.getClientPublicKeyOrId();
        this.clientSecretKey = bootstrapFromCredential.getClientSecretKey();
        this.serverPublicKey = profileServerBootstrap.getServerPublicKey();
        this.clientHoldOffTime = profileServerBootstrap.getClientHoldOffTime();
        this.bootstrapServerAccountTimeout = profileServerBootstrap.getBootstrapServerAccountTimeout();
        this.host = (profileServerBootstrap.getHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getHost();
        this.port = profileServerBootstrap.getPort();
        this.securityHost = (profileServerBootstrap.getSecurityHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getSecurityHost();
        this.securityPort = profileServerBootstrap.getSecurityPort();
        this.securityMode = profileServerBootstrap.getSecurityMode();
        this.serverId = profileServerBootstrap.getServerId();
        this.bootstrapServerIs = profileServerBootstrap.bootstrapServerIs;
    }
}
