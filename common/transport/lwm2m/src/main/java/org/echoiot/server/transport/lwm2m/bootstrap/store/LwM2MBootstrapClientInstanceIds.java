package org.echoiot.server.transport.lwm2m.bootstrap.store;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class LwM2MBootstrapClientInstanceIds {

    /**
     * Map<serverId (shortId), InstanceId>
     */
    @NotNull
    private Map<Integer, Integer> securityInstances = new HashMap<>();
    @NotNull
    private  Map<Integer, Integer> serverInstances = new HashMap<>();
}
