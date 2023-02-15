package org.thingsboard.rule.engine.profile.state;

import lombok.Data;

import java.util.Map;

@Data
public class PersistedDeviceState {

    Map<String, PersistedAlarmState> alarmStates;

}
