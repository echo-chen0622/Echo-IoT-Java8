package org.echoiot.server.service.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.echoiot.server.common.msg.rpc.ToDeviceRpcRequest;

/**
 * Created by Echo on 16.04.18.
 */
@ToString
@RequiredArgsConstructor
public class ToDeviceRpcRequestActorMsg implements ToDeviceActorNotificationMsg {

    private static final long serialVersionUID = -8592877558138716589L;

    @Getter
    private final String serviceId;
    @Getter
    private final ToDeviceRpcRequest msg;

    @Override
    public DeviceId getDeviceId() {
        return msg.getDeviceId();
    }

    @Override
    public TenantId getTenantId() {
        return msg.getTenantId();
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG;
    }
}
