package org.echoiot.server.transport.lwm2m.server.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import static org.echoiot.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MTelemetryLogService implements LwM2MTelemetryLogService {

    @NotNull
    private final LwM2mTransportServerHelper helper;

    @Override
    public void log(@Nullable LwM2mClient client, @Nullable String logMsg) {
        if (logMsg != null && client != null && client.getSession() != null) {
            if (logMsg.length() > 1024) {
                logMsg = logMsg.substring(0, 1024);
            }
            this.helper.sendParametersOnEchoiotTelemetry(this.helper.getKvStringtoEchoiot(LOG_LWM2M_TELEMETRY, logMsg), client.getSession(), client.getKeyTsLatestMap());
        }
    }

}
