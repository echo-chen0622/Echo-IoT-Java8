package org.echoiot.server.transport.lwm2m.secure;

import lombok.Data;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;

import java.io.Serializable;

@Data
public class TbX509DtlsSessionInfo implements Serializable {

    private final String x509CommonName;
    private final ValidateDeviceCredentialsResponse credentials;

}
