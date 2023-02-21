package org.echoiot.server.service.state;

import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 01.05.18.
 */
@Data
@Builder
class DeviceStateData {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final CustomerId customerId;
    @NotNull
    private final DeviceId deviceId;
    private final long deviceCreationTime;
    private TbMsgMetaData metaData;
    @NotNull
    private final DeviceState state;

}
