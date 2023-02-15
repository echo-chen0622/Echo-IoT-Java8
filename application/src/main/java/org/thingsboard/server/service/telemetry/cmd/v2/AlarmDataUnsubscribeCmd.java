package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;

@Data
public class AlarmDataUnsubscribeCmd implements UnsubscribeCmd {

    private final int cmdId;

}
