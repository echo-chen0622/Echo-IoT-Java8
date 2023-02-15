package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;

import java.util.List;

@Data
public class AggHistoryCmd {

    private List<AggKey> keys;
    private long startTs;
    private long endTs;

}
