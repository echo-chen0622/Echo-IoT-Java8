package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Data
public class TbCheckAlarmStatusNodeConfig implements NodeConfiguration<TbCheckAlarmStatusNodeConfig> {
    private List<AlarmStatus> alarmStatusList;

    @NotNull
    @Override
    public TbCheckAlarmStatusNodeConfig defaultConfiguration() {
        @NotNull TbCheckAlarmStatusNodeConfig config = new TbCheckAlarmStatusNodeConfig();
        config.setAlarmStatusList(Arrays.asList(AlarmStatus.ACTIVE_ACK, AlarmStatus.ACTIVE_UNACK));
        return config;
    }
}
