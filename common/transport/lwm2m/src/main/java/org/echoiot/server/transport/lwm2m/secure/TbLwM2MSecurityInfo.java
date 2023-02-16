package org.echoiot.server.transport.lwm2m.secure;

import lombok.Data;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.security.SecurityInfo;

import java.io.Serializable;

@Data
public class TbLwM2MSecurityInfo implements Serializable {
    private ValidateDeviceCredentialsResponse msg;
    private DeviceProfile deviceProfile;
    private String endpoint;
    private SecurityInfo securityInfo;
    private SecurityMode securityMode;


    /** bootstrap */
    private LwM2MBootstrapConfig bootstrapCredentialConfig;
    private BootstrapConfig bootstrapConfig;
}
