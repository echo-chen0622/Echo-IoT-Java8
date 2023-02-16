package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmInfo;
import org.echoiot.server.dao.util.mapping.JsonStringType;

import javax.persistence.Entity;
import javax.persistence.Table;

import static org.echoiot.server.dao.model.ModelConstants.ALARM_COLUMN_FAMILY_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ALARM_COLUMN_FAMILY_NAME)
public final class AlarmEntity extends AbstractAlarmEntity<Alarm> {

    public AlarmEntity() {
        super();
    }

    public AlarmEntity(AlarmInfo alarmInfo) {
        super(alarmInfo);
    }

    public AlarmEntity(Alarm alarm) {
        super(alarm);
    }

    @Override
    public Alarm toData() {
        return super.toAlarm();
    }
}
