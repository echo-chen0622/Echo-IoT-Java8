package org.echoiot.server.dao.device;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
class DeviceCredentialsEvictEvent {

    @NotNull
    private final String newCedentialsId;
    @NotNull
    private final String oldCredentialsId;

}
