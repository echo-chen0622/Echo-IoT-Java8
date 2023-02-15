package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name="customer attributes",
        configClazz = TbGetEntityAttrNodeConfiguration.class,
        nodeDescription = "Add Originators Customer Attributes or Latest Telemetry into Message Metadata",
        nodeDetails = "If Attributes enrichment configured, server scope attributes are added into Message metadata. " +
                "If Latest Telemetry enrichment configured, latest telemetry added into metadata. " +
                "To access those attributes in other nodes this template can be used " +
                "<code>metadata.temperature</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeCustomerAttributesConfig")
public class TbGetCustomerAttributeNode extends TbEntityGetAttrNode<CustomerId> {

    @Override
    protected ListenableFuture<CustomerId> findEntityAsync(TbContext ctx, EntityId originator) {
        return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, originator);
    }

}
