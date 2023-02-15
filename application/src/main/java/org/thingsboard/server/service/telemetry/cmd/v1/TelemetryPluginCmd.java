package org.thingsboard.server.service.telemetry.cmd.v1;

/**
 * @author Andrew Shvayka
 */
public interface TelemetryPluginCmd {

    int getCmdId();

    void setCmdId(int cmdId);

    String getKeys();

}
