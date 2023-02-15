package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
public class DataCmd {

    @Getter
    private final int cmdId;

    public DataCmd(int cmdId) {
        this.cmdId = cmdId;
    }

}
