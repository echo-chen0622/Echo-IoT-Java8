package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineAssetProfileCache {

    @Nullable
    AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId);

    @Nullable
    AssetProfile get(TenantId tenantId, AssetId assetId);

    void addListener(TenantId tenantId, EntityId listenerId, Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetlistener);

    void removeListener(TenantId tenantId, EntityId listenerId);

}
