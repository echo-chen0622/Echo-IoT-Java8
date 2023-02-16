package org.echoiot.server.common.msg.aware;

import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.msg.TbActorMsg;

public interface RuleChainAwareMsg extends TbActorMsg {

	RuleChainId getRuleChainId();

}
