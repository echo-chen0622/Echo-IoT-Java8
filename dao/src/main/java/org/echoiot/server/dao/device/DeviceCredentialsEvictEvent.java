package org.echoiot.server.dao.device;

import lombok.Data;

@Data
class DeviceCredentialsEvictEvent {

    private final String newCedentialsId;
    private final String oldCredentialsId;

}
