package org.echoiot.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.echoiot.server.common.data.query.FilterPredicateValue;
import org.jetbrains.annotations.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepeatingAlarmConditionSpec implements AlarmConditionSpec {

    private FilterPredicateValue<Integer> predicate;

    @NotNull
    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.REPEATING;
    }
}
