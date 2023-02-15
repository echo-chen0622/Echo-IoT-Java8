package org.thingsboard.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CmdUpdate {

    private final int cmdId;
    private final int errorCode;
    private final String errorMsg;

    public abstract CmdUpdateType getCmdUpdateType();

}
