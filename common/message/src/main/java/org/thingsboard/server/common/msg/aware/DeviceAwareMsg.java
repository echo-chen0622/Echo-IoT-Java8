package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.TbActorMsg;

public interface DeviceAwareMsg extends TbActorMsg {

    DeviceId getDeviceId();
}
