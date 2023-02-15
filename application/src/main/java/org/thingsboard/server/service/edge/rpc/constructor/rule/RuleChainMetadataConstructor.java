package org.thingsboard.server.service.edge.rpc.constructor.rule;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

public interface RuleChainMetadataConstructor {

    RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                                    UpdateMsgType msgType,
                                                                    RuleChainMetaData ruleChainMetaData);
}
