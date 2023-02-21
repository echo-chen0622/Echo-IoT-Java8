package org.echoiot.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class LwM2MBootstrapServers {
    @NotNull
    private Integer shortId = 123;
    @NotNull
    private Integer lifetime = 300;
    @NotNull
    private Integer defaultMinPeriod = 1;
    private boolean notifIfDisabled = true;
    @NotNull
    private String binding = "UQ";
}
