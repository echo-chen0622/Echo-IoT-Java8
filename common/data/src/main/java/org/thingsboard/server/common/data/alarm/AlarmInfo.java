package org.thingsboard.server.common.data.alarm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AlarmInfo extends Alarm {

    private static final long serialVersionUID = 2807343093519543363L;

    @ApiModelProperty(position = 19, value = "Alarm originator name", example = "Thermostat")
    private String originatorName;

    public AlarmInfo() {
        super();
    }

    public AlarmInfo(Alarm alarm) {
        super(alarm);
    }

    public AlarmInfo(Alarm alarm, String originatorName) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AlarmInfo alarmInfo = (AlarmInfo) o;

        return originatorName != null ? originatorName.equals(alarmInfo.originatorName) : alarmInfo.originatorName == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (originatorName != null ? originatorName.hashCode() : 0);
        return result;
    }
}
