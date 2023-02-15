package org.thingsboard.server.common.data.device.profile;

import lombok.Data;

@Data
public class ProvisionDeviceProfileCredentials {
    private final String provisionDeviceKey;
    private final String provisionDeviceSecret;
}
