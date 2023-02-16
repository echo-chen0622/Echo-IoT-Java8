package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;

/**
 * Created by Echo on 19.01.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
          name = "originator attributes",
          configClazz = TbGetAttributesNodeConfiguration.class,
          nodeDescription = "Add Message Originator Attributes or Latest Telemetry into Message Data or Metadata",
          nodeDetails = "If Attributes enrichment configured, <b>CLIENT/SHARED/SERVER</b> attributes are added into Message data/metadata " +
                "with specific prefix: <i>cs/shared/ss</i>. Latest telemetry value added into Message data/metadata without prefix. " +
                  "To access those attributes in other nodes this template can be used " +
                "<code>metadata.cs_temperature</code> or <code>metadata.shared_limit</code> ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeOriginatorAttributesConfig")
public class TbGetAttributesNode extends TbAbstractGetAttributesNode<TbGetAttributesNodeConfiguration, EntityId> {

    @Override
    protected TbGetAttributesNodeConfiguration loadGetAttributesNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetAttributesNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<EntityId> findEntityIdAsync(TbContext ctx, TbMsg msg) {
        return Futures.immediateFuture(msg.getOriginator());
    }

}
