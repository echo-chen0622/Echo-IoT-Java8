package org.echoiot.server.service.edge.rpc.constructor.rule;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

public interface RuleChainMetadataConstructor {

    RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                    UpdateMsgType msgType,
                                                                    RuleChainMetaData ruleChainMetaData);
}
