package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineAssetProfileCache {

    AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId);

    AssetProfile get(TenantId tenantId, AssetId assetId);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetlistener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
