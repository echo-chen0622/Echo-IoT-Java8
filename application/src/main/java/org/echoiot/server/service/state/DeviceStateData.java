package org.echoiot.server.service.state;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsgMetaData;

/**
 * Created by Echo on 01.05.18.
 */
@Data
@Builder
class DeviceStateData {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final long deviceCreationTime;
    private TbMsgMetaData metaData;
    private final DeviceState state;

}