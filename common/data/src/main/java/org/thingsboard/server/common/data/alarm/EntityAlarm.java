package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityAlarm implements HasTenantId {

    private TenantId tenantId;
    private EntityId entityId;
    private long createdTime;
    private String alarmType;

    private CustomerId customerId;
    private AlarmId alarmId;

}
