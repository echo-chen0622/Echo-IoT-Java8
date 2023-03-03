package org.echoiot.server.service.telemetry.cmd.v1;

/**
 * @author Echo
 */
public interface TelemetryPluginCmd {

    int getCmdId();

    void setCmdId(int cmdId);

    String getKeys();

}
