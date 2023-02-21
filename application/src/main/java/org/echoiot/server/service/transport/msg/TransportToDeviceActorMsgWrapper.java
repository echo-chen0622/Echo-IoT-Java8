package org.echoiot.server.service.transport.msg;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.echoiot.server.common.msg.aware.DeviceAwareMsg;
import org.echoiot.server.common.msg.aware.TenantAwareMsg;
import org.echoiot.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Echo on 09.10.18.
 */
@Data
public class TransportToDeviceActorMsgWrapper implements TbActorMsg, DeviceAwareMsg, TenantAwareMsg, Serializable {

    private static final long serialVersionUID = 7191333353202935941L;

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final DeviceId deviceId;
    private final TransportToDeviceActorMsg msg;
    private final TbCallback callback;

    public TransportToDeviceActorMsgWrapper(@NotNull TransportToDeviceActorMsg msg, TbCallback callback) {
        this.msg = msg;
        this.callback = callback;
        this.tenantId = TenantId.fromUUID(new UUID(msg.getSessionInfo().getTenantIdMSB(), msg.getSessionInfo().getTenantIdLSB()));
        this.deviceId = new DeviceId(new UUID(msg.getSessionInfo().getDeviceIdMSB(), msg.getSessionInfo().getDeviceIdLSB()));
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.TRANSPORT_TO_DEVICE_ACTOR_MSG;
    }
}
