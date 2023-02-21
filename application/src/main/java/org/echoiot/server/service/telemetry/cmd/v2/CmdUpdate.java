package org.echoiot.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CmdUpdate {

    private final int cmdId;
    private final int errorCode;
    @NotNull
    private final String errorMsg;

    public abstract CmdUpdateType getCmdUpdateType();

}
