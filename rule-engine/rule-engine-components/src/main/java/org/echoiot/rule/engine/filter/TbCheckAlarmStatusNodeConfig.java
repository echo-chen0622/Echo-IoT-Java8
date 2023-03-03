package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.alarm.AlarmStatus;

import java.util.Arrays;
import java.util.List;

@Data
public class TbCheckAlarmStatusNodeConfig implements NodeConfiguration {
    private List<AlarmStatus> alarmStatusList;

    @Override
    public TbCheckAlarmStatusNodeConfig defaultConfiguration() {
        TbCheckAlarmStatusNodeConfig config = new TbCheckAlarmStatusNodeConfig();
        config.setAlarmStatusList(Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK));
        return config;
    }
}
