package org.echoiot.server.service.telemetry.cmd;

import lombok.Data;
import org.echoiot.server.service.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.echoiot.server.service.telemetry.cmd.v1.GetHistoryCmd;
import org.echoiot.server.service.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.echoiot.server.service.telemetry.cmd.v2.*;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
@Data
public class TelemetryPluginCmdsWrapper {

    private List<AttributesSubscriptionCmd> attrSubCmds;

    private List<TimeseriesSubscriptionCmd> tsSubCmds;

    private List<GetHistoryCmd> historyCmds;

    private List<EntityDataCmd> entityDataCmds;

    private List<EntityDataUnsubscribeCmd> entityDataUnsubscribeCmds;

    private List<AlarmDataCmd> alarmDataCmds;

    private List<AlarmDataUnsubscribeCmd> alarmDataUnsubscribeCmds;

    private List<EntityCountCmd> entityCountCmds;

    private List<EntityCountUnsubscribeCmd> entityCountUnsubscribeCmds;

}
