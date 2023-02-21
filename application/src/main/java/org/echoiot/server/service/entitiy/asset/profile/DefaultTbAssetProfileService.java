package org.echoiot.server.service.entitiy.asset.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.BaseAssetService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbAssetProfileService extends AbstractTbEntityService implements TbAssetProfileService {

    @NotNull
    private final AssetProfileService assetProfileService;

    @NotNull
    @Override
    public AssetProfile save(@NotNull AssetProfile assetProfile, User user) throws Exception {
        @NotNull ActionType actionType = assetProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = assetProfile.getTenantId();
        try {
            if (BaseAssetService.TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                throw new EchoiotException("Unable to save asset profile with name " + BaseAssetService.TB_SERVICE_QUEUE, EchoiotErrorCode.BAD_REQUEST_PARAMS);
            } else if (assetProfile.getId() != null) {
                AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfile.getId());
                if (foundAssetProfile != null && BaseAssetService.TB_SERVICE_QUEUE.equals(foundAssetProfile.getName())) {
                    throw new EchoiotException("Updating asset profile with name " + BaseAssetService.TB_SERVICE_QUEUE + " is prohibited!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            AssetProfile savedAssetProfile = checkNotNull(assetProfileService.saveAssetProfile(assetProfile));
            autoCommit(user, savedAssetProfile.getId());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAssetProfile.getId(),
                    actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, savedAssetProfile.getId(),
                    savedAssetProfile, user, actionType, true, null);
            return savedAssetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), assetProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(@NotNull AssetProfile assetProfile, User user) {
        AssetProfileId assetProfileId = assetProfile.getId();
        TenantId tenantId = assetProfile.getTenantId();
        try {
            assetProfileService.deleteAssetProfile(tenantId, assetProfileId);

            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId, ComponentLifecycleEvent.DELETED);
            notificationEntityService.notifyCreateOrUpdateOrDelete(tenantId, null, assetProfileId, assetProfile,
                    user, ActionType.DELETED, true, null, assetProfileId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), ActionType.DELETED,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }

    @Override
    public AssetProfile setDefaultAssetProfile(@NotNull AssetProfile assetProfile, @Nullable AssetProfile previousDefaultAssetProfile, User user) throws EchoiotException {
        TenantId tenantId = assetProfile.getTenantId();
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            if (assetProfileService.setDefaultAssetProfile(tenantId, assetProfileId)) {
                if (previousDefaultAssetProfile != null) {
                    previousDefaultAssetProfile = assetProfileService.findAssetProfileById(tenantId, previousDefaultAssetProfile.getId());
                    notificationEntityService.logEntityAction(tenantId, previousDefaultAssetProfile.getId(), previousDefaultAssetProfile,
                            ActionType.UPDATED, user);
                }
                assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);

                notificationEntityService.logEntityAction(tenantId, assetProfileId, assetProfile, ActionType.UPDATED, user);
            }
            return assetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), ActionType.UPDATED,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }
}
