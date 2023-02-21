package org.echoiot.server.common.data.alarm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@ApiModel
public class AlarmInfo extends Alarm {

    private static final long serialVersionUID = 2807343093519543363L;

    @ApiModelProperty(position = 19, value = "Alarm originator name", example = "Thermostat")
    private String originatorName;

    public AlarmInfo() {
        super();
    }

    public AlarmInfo(@NotNull Alarm alarm) {
        super(alarm);
    }

    public AlarmInfo(@NotNull Alarm alarm, String originatorName) {
        super(alarm);
        this.originatorName = originatorName;
    }

    public String getOriginatorName() {
        return originatorName;
    }

    public void setOriginatorName(String originatorName) {
        this.originatorName = originatorName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        @NotNull AlarmInfo alarmInfo = (AlarmInfo) o;

        return Objects.equals(originatorName, alarmInfo.originatorName);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (originatorName != null ? originatorName.hashCode() : 0);
        return result;
    }
}
