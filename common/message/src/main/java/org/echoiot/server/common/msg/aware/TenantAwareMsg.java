package org.echoiot.server.common.msg.aware;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbActorMsg;

public interface TenantAwareMsg extends TbActorMsg {

	TenantId getTenantId();

}
