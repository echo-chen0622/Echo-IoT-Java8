package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.msg.TbActorMsg;

public interface RuleChainAwareMsg extends TbActorMsg {

	RuleChainId getRuleChainId();

}
