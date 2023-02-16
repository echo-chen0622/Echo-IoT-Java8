package org.echoiot.server.common.msg.aware;

import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.msg.TbActorMsg;

public interface DeviceAwareMsg extends TbActorMsg {

    DeviceId getDeviceId();
}
