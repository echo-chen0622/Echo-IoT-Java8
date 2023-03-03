package org.echoiot.rule.engine.api.msg;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;

/**
 * @author Echo
 */
@Data
public class DeviceCredentialsUpdateNotificationMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -3956907402411126990L;

    private final TenantId tenantId;
    private final DeviceId deviceId;

    /**
     * LwM2M
     * @return
     */
    private final DeviceCredentials deviceCredentials;

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
