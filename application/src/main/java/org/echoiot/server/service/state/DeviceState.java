package org.echoiot.server.service.state;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Echo on 01.05.18.
 */
@Data
@Builder
public class DeviceState {

    private boolean active;
    private long lastConnectTime;
    private long lastActivityTime;
    private long lastDisconnectTime;
    private long lastInactivityAlarmTime;
    private long inactivityTimeout;

}
