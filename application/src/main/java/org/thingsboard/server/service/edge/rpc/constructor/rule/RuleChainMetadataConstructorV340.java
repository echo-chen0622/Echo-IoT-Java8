package org.thingsboard.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;

import java.util.TreeSet;

@Slf4j
public class RuleChainMetadataConstructorV340 extends AbstractRuleChainMetadataConstructor {

    @Override
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        RuleChainMetadataUpdateMsg.Builder builder,
                                                        RuleChainMetaData ruleChainMetaData) throws JsonProcessingException {
        builder.addAllNodes(constructNodes(ruleChainMetaData.getNodes()))
                .addAllConnections(constructConnections(ruleChainMetaData.getConnections()))
                .addAllRuleChainConnections(constructRuleChainConnections(ruleChainMetaData.getRuleChainConnections(), new TreeSet<>()));
        if (ruleChainMetaData.getFirstNodeIndex() != null) {
            builder.setFirstNodeIndex(ruleChainMetaData.getFirstNodeIndex());
        } else {
            builder.setFirstNodeIndex(-1);
        }
    }
}
