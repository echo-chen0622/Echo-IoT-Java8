package org.echoiot.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleAlarmConditionSpec implements AlarmConditionSpec {
    @NotNull
    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.SIMPLE;
    }
}
