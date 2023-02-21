package org.echoiot.server.service.edge.rpc.constructor.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.jetbrains.annotations.NotNull;

import java.util.TreeSet;

@Slf4j
public class RuleChainMetadataConstructorV340 extends AbstractRuleChainMetadataConstructor {

    @Override
    protected void constructRuleChainMetadataUpdatedMsg(TenantId tenantId,
                                                        @NotNull RuleChainMetadataUpdateMsg.Builder builder,
                                                        @NotNull RuleChainMetaData ruleChainMetaData) throws JsonProcessingException {
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
