package org.echoiot.server.transport.lwm2m.server.log;

import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;

public interface LwM2MTelemetryLogService {

    void log(LwM2mClient client, String msg);

}