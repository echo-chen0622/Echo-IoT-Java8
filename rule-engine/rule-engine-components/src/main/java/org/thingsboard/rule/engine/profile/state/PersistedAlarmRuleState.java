package org.thingsboard.rule.engine.profile.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersistedAlarmRuleState {

    private long lastEventTs;
    private long duration;
    private long eventCount;

}
