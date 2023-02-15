package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.alarm.AlarmInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmInfoEntity extends AbstractAlarmEntity<AlarmInfo> {

    private String originatorName;

    public AlarmInfoEntity() {
        super();
    }

    public AlarmInfoEntity(AlarmEntity alarmEntity) {
        super(alarmEntity);
    }

    @Override
    public AlarmInfo toData() {
        return new AlarmInfo(super.toAlarm(), this.originatorName);
    }
}
