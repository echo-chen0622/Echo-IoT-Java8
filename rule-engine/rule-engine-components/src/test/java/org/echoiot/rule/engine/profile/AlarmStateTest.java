package org.echoiot.rule.engine.profile;

import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AlarmStateTest {

    @Test
    public void testSetAlarmConditionMetadata_repeatingCondition() {
        @NotNull AlarmRuleState ruleState = createMockAlarmRuleState(new RepeatingAlarmConditionSpec());
        int eventCount = 3;
        ruleState.getState().setEventCount(eventCount);

        @NotNull AlarmState alarmState = createMockAlarmState();
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.REPEATING, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertEquals(String.valueOf(eventCount), metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
    }

    @Test
    public void testSetAlarmConditionMetadata_durationCondition() {
        @NotNull DurationAlarmConditionSpec spec = new DurationAlarmConditionSpec();
        spec.setUnit(TimeUnit.SECONDS);
        @NotNull AlarmRuleState ruleState = createMockAlarmRuleState(spec);
        int duration = 12;
        ruleState.getState().setDuration(duration);

        @NotNull AlarmState alarmState = createMockAlarmState();
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();

        alarmState.setAlarmConditionMetadata(ruleState, metaData);

        assertEquals(AlarmConditionSpecType.DURATION, ruleState.getSpec().getType());
        assertNotNull(metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
        assertNull(metaData.getValue(DataConstants.ALARM_CONDITION_REPEATS));
        assertEquals(String.valueOf(duration), metaData.getValue(DataConstants.ALARM_CONDITION_DURATION));
    }

    @NotNull
    private AlarmRuleState createMockAlarmRuleState(AlarmConditionSpec spec) {
        @NotNull AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(spec);

        @NotNull AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);

        return new AlarmRuleState(null, alarmRule, null, null, null);
    }

    @NotNull
    private AlarmState createMockAlarmState() {
        return new AlarmState(null, null, mock(DeviceProfileAlarm.class), null, null);
    }
}
