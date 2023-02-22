package org.echoiot.rule.engine.filter;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.EmptyNodeConfiguration;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "asset profile switch",
        customRelations = true,
        relationTypes = {},
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Route incoming messages based on the name of the asset profile",
        nodeDetails = "Route incoming messages based on the name of the asset profile. The asset profile name is case-sensitive",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbAssetTypeSwitchNode extends TbAbstractTypeSwitchNode {

    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) throws TbNodeException {
        if (!EntityType.ASSET.equals(originator.getEntityType())) {
            throw new TbNodeException("Unsupported originator type: " + originator.getEntityType() + "! Only 'ASSET' type is allowed.");
        }
        @Nullable AssetProfile assetProfile = ctx.getAssetProfileCache().get(ctx.getTenantId(), (AssetId) originator);
        if (assetProfile == null) {
            throw new TbNodeException("Asset profile for entity id: " + originator.getId() + " wasn't found!");
        }
        return assetProfile.getName();
    }

}
