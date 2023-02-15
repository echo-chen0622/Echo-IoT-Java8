package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;

public interface TenantAwareMsg extends TbActorMsg {

	TenantId getTenantId();

}
