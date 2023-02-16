package org.echoiot.server.service.entitiy.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.ThingsboardErrorCode;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.asset.BaseAssetService;
import org.springframework.stereotype.Service;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.echoiot.server.service.profile.TbAssetProfileCache;

import java.util.List;

@Service
@AllArgsConstructor
public class DefaultTbAssetService extends AbstractTbEntityService implements TbAssetService {

    private final AssetService assetService;
    private final TbAssetProfileCache assetProfileCache;

    @Override
    public Asset save(Asset asset, User user) throws Exception {
        ActionType actionType = asset.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = asset.getTenantId();
        try {
            if (BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
                throw new ThingsboardException("Unable to save asset with type " + BaseAssetService.TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (asset.getAssetProfileId() != null) {
                AssetProfile assetProfile = assetProfileCache.get(tenantId, asset.getAssetProfileId());
                if (assetProfile != null && BaseAssetService.TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                    throw new ThingsboardException("Unable to save asset with profile " + BaseAssetService.TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            Asset savedAsset = checkNotNull(assetService.saveAsset(asset));
            autoCommit(user, savedAsset.getId());
            notificationEntityService.notifyCreateOrUpdateEntity(tenantId, savedAsset.getId(), savedAsset,
                    asset.getCustomerId(), actionType, user);
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAsset.getId(),
                    asset.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), asset, actionType, user, e);
            throw e;
        }
    }

    @Override
    public ListenableFuture<Void> delete(Asset asset, User user) {
        TenantId tenantId = asset.getTenantId();
        AssetId assetId = asset.getId();
        try {
            List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, assetId);
            assetService.deleteAsset(tenantId, assetId);
            notificationEntityService.notifyDeleteEntity(tenantId, assetId, asset, asset.getCustomerId(),
                    ActionType.DELETED, relatedEdgeIds, user, assetId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetId, ComponentLifecycleEvent.DELETED);
            return removeAlarmsByEntityId(tenantId, assetId);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), ActionType.DELETED, user, e,
                    assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, user, true, assetId.toString(), customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e,
                    assetId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromCustomer(tenantId, assetId));
            CustomerId customerId = customer.getId();
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, customerId, savedAsset,
                    actionType, user, true, assetId.toString(), customerId.toString(), customer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToPublicCustomer(TenantId tenantId, AssetId assetId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
            Asset savedAsset = checkNotNull(assetService.assignAssetToCustomer(tenantId, assetId, publicCustomer.getId()));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, assetId, savedAsset.getCustomerId(), savedAsset,
                    actionType, user, false, actionType.toString(), publicCustomer.getId().toString(), publicCustomer.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType, user, e, assetId.toString());
            throw e;
        }
    }

    @Override
    public Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.assignAssetToEdge(tenantId, assetId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, savedAsset.getCustomerId(),
                    edgeId, savedAsset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        AssetId assetId = asset.getId();
        EdgeId edgeId = edge.getId();
        try {
            Asset savedAsset = checkNotNull(assetService.unassignAssetFromEdge(tenantId, assetId, edgeId));

            notificationEntityService.notifyAssignOrUnassignEntityToEdge(tenantId, assetId, asset.getCustomerId(),
                    edgeId, asset, actionType, user, assetId.toString(), edgeId.toString(), edge.getName());

            return savedAsset;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET), actionType,
                    user, e, assetId.toString(), edgeId.toString());
            throw e;
        }
    }
}
