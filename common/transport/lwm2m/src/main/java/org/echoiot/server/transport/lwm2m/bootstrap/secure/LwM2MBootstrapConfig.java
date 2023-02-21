package org.echoiot.server.transport.lwm2m.bootstrap.secure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.device.credentials.lwm2m.AbstractLwM2MBootstrapClientCredentialWithKeys;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredential;
import org.echoiot.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Data
public class LwM2MBootstrapConfig implements Serializable {

    private static final long serialVersionUID = -4729088085817468640L;

    List<LwM2MBootstrapServerCredential> serverConfiguration;

    /** -bootstrapServer, lwm2mServer
     * interface ServerSecurityConfig
     *   host?: string,
     *   port?: number,
     *   isBootstrapServer?: boolean,
     *   securityMode: string,
     *   clientPublicKeyOrId?: string,
     *   clientSecretKey?: string,
     *   serverPublicKey?: string;
     *   clientHoldOffTime?: number,
     *   serverId?: number,
     *   bootstrapServerAccountTimeout: number
     * */
    @Getter
    @Setter
    private LwM2MBootstrapClientCredential bootstrapServer;

    @Getter
    @Setter
    private LwM2MBootstrapClientCredential lwm2mServer;

    public LwM2MBootstrapConfig(){}

    public LwM2MBootstrapConfig(List<LwM2MBootstrapServerCredential> serverConfiguration, LwM2MBootstrapClientCredential bootstrapClientServer, LwM2MBootstrapClientCredential lwm2mClientServer) {
        this.serverConfiguration = serverConfiguration;
        this.bootstrapServer = bootstrapClientServer;
        this.lwm2mServer = lwm2mClientServer;

    }

    @NotNull
    @JsonIgnore
    public BootstrapConfig getLwM2MBootstrapConfig() {
        @NotNull BootstrapConfig configBs = new BootstrapConfig();
        configBs.autoIdForSecurityObject = true;
        int id = 0;
        for (@NotNull LwM2MBootstrapServerCredential serverCredential : serverConfiguration) {
            @NotNull BootstrapConfig.ServerConfig serverConfig = setServerConfig((AbstractLwM2MBootstrapServerCredential) serverCredential);
            configBs.servers.put(id, serverConfig);
            @NotNull BootstrapConfig.ServerSecurity serverSecurity = setServerSecurity((AbstractLwM2MBootstrapServerCredential) serverCredential, serverCredential.getSecurityMode());
            configBs.security.put(id, serverSecurity);
            id++;
        }
        /** in LwM2mDefaultBootstrapSessionManager -> initTasks
         * Delete all security/config objects if update bootstrap server and lwm2m server
         * if other: del or update only instances */

        return configBs;
    }

    @NotNull
    private BootstrapConfig.ServerSecurity setServerSecurity(@NotNull AbstractLwM2MBootstrapServerCredential serverCredential, @NotNull LwM2MSecurityMode securityMode) {
        @NotNull BootstrapConfig.ServerSecurity serverSecurity = new BootstrapConfig.ServerSecurity();
        @NotNull String serverUri = "coap://";
        byte[] publicKeyOrId = new byte[]{};
        byte[] secretKey = new byte[]{};
        byte[] serverPublicKey = new byte[]{};
        serverSecurity.serverId = serverCredential.getShortServerId();
        serverSecurity.securityMode = SecurityMode.valueOf(securityMode.name());
        serverSecurity.bootstrapServer = serverCredential.isBootstrapServerIs();
        if (!LwM2MSecurityMode.NO_SEC.equals(securityMode)) {
            AbstractLwM2MBootstrapClientCredentialWithKeys server;
            if (serverSecurity.bootstrapServer) {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.bootstrapServer;

            } else {
                server = (AbstractLwM2MBootstrapClientCredentialWithKeys) this.lwm2mServer;
            }
            serverUri = "coaps://";
            if (LwM2MSecurityMode.PSK.equals(securityMode)) {
                publicKeyOrId = server.getClientPublicKeyOrId().getBytes();
                secretKey = Hex.decodeHex(server.getClientSecretKey().toCharArray());
            } else {
                publicKeyOrId = server.getDecodedClientPublicKeyOrId();
                secretKey = server.getDecodedClientSecretKey();
            }
            serverPublicKey = serverCredential.getDecodedCServerPublicKey();
        }
        serverUri += (((serverCredential.getHost().equals("0.0.0.0") ? "localhost" : serverCredential.getHost()) + ":" + serverCredential.getPort()));
        serverSecurity.uri = serverUri;
        serverSecurity.publicKeyOrId = publicKeyOrId;
        serverSecurity.secretKey = secretKey;
        serverSecurity.serverPublicKey = serverPublicKey;
        return serverSecurity;
    }

    @NotNull
    private BootstrapConfig.ServerConfig setServerConfig(@NotNull AbstractLwM2MBootstrapServerCredential serverCredential) {
        @NotNull BootstrapConfig.ServerConfig serverConfig = new BootstrapConfig.ServerConfig();
        serverConfig.shortId = serverCredential.getShortServerId();
        serverConfig.lifetime = serverCredential.getLifetime();
        serverConfig.defaultMinPeriod = serverCredential.getDefaultMinPeriod();
        serverConfig.notifIfDisabled = serverCredential.isNotifIfDisabled();
        serverConfig.binding = BindingMode.parse(serverCredential.getBinding());
        return serverConfig;
    }
}
