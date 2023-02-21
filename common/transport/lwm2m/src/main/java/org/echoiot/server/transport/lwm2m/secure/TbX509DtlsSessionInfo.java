package org.echoiot.server.transport.lwm2m.secure;

import lombok.Data;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Data
public class TbX509DtlsSessionInfo implements Serializable {

    @NotNull
    private final String x509CommonName;
    @NotNull
    private final ValidateDeviceCredentialsResponse credentials;

}
