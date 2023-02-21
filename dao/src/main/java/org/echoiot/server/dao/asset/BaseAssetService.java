package org.echoiot.server.dao.asset;


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetInfo;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetSearchQuery;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.echoiot.server.dao.DaoUtil.toUUIDs;
import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAssetService extends AbstractCachedEntityService<AssetCacheKey, Asset, AssetCacheEvictEvent> implements AssetService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    public static final String INCORRECT_ASSET_PROFILE_ID = "Incorrect assetProfileId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";
    public static final String TB_SERVICE_QUEUE = "TbServiceQueue";

    @Resource
    private AssetDao assetDao;

    @Resource
    private AssetProfileService assetProfileService;

    @Resource
    private DataValidator<Asset> assetValidator;

    @TransactionalEventListener(classes = AssetCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull AssetCacheEvictEvent event) {
        @NotNull List<AssetCacheKey> keys = new ArrayList<>(2);
        keys.add(new AssetCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new AssetCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, @NotNull AssetId assetId) {
        log.trace("Executing findAssetInfoById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findAssetInfoById(tenantId, assetId.getId());
    }

    @Override
    public Asset findAssetById(TenantId tenantId, @NotNull AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findById(tenantId, assetId.getId());
    }

    @Override
    public ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, @NotNull AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findByIdAsync(tenantId, assetId.getId());
    }

    @Override
    public Asset findAssetByTenantIdAndName(@NotNull TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new AssetCacheKey(tenantId, name),
                () -> assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    @Override
    public Asset saveAsset(@NotNull Asset asset) {
        log.trace("Executing saveAsset [{}]", asset);
        Asset oldAsset = assetValidator.validate(asset, Asset::getTenantId);
        Asset savedAsset;
        @NotNull AssetCacheEvictEvent evictEvent = new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), oldAsset != null ? oldAsset.getName() : null);
        try {
            AssetProfile assetProfile;
            if (asset.getAssetProfileId() == null) {
                if (!StringUtils.isEmpty(asset.getType())) {
                    assetProfile = this.assetProfileService.findOrCreateAssetProfile(asset.getTenantId(), asset.getType());
                } else {
                    assetProfile = this.assetProfileService.findDefaultAssetProfile(asset.getTenantId());
                }
                asset.setAssetProfileId(new AssetProfileId(assetProfile.getId().getId()));
            } else {
                assetProfile = this.assetProfileService.findAssetProfileById(asset.getTenantId(), asset.getAssetProfileId());
                if (assetProfile == null) {
                    throw new DataValidationException("Asset is referencing non existing asset profile!");
                }
                if (!assetProfile.getTenantId().equals(asset.getTenantId())) {
                    throw new DataValidationException("Asset can`t be referencing to asset profile from different tenant!");
                }
            }
            asset.setType(assetProfile.getName());
            savedAsset = assetDao.saveAndFlush(asset.getTenantId(), asset);
            publishEvictEvent(evictEvent);
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t,
                    "asset_name_unq_key", "Asset with such name already exists!",
                    "asset_external_id_unq_key", "Asset with such external id already exists!");
            throw t;
        }
        return savedAsset;
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, @NotNull AssetId assetId, CustomerId customerId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(customerId);
        return saveAsset(asset);
    }

    @Override
    public Asset unassignAssetFromCustomer(TenantId tenantId, @NotNull AssetId assetId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(null);
        return saveAsset(asset);
    }

    @Override
    @Transactional
    public void deleteAsset(TenantId tenantId, @NotNull AssetId assetId) {
        log.trace("Executing deleteAsset [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        deleteEntityRelations(tenantId, assetId);

        Asset asset = assetDao.findById(tenantId, assetId.getId());
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(asset.getTenantId(), assetId);
        if (entityViews != null && !entityViews.isEmpty()) {
            throw new DataValidationException("Can't delete asset that has entity views!");
        }

        publishEvictEvent(new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), null));

        assetDao.removeById(tenantId, assetId.getId());
    }

    @Override
    public PageData<Asset> findAssetsByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(@NotNull TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(@NotNull TenantId tenantId, @NotNull AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndAssetProfileId, tenantId [{}], assetProfileId [{}], pageLink [{}]", tenantId, assetProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndAssetProfileId(tenantId.getId(), assetProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void deleteAssetsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(@NotNull TenantId tenantId, @NotNull CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull AssetProfileId assetProfileId, PageLink pageLink) {
        log.trace("Executing findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId, tenantId [{}], customerId [{}], assetProfileId [{}], pageLink [{}]", tenantId, customerId, assetProfileId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId.getId(), customerId.getId(), assetProfileId.getId(), pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(@NotNull TenantId tenantId, @NotNull CustomerId customerId, @NotNull List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]", tenantId, customerId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Validator.validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerAssetsUnasigner.removeEntities(tenantId, customerId);
    }

    @NotNull
    @Override
    public ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, @NotNull AssetSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        @NotNull ListenableFuture<List<Asset>> assets = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            @NotNull List<ListenableFuture<Asset>> futures = new ArrayList<>();
            for (@NotNull EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ASSET) {
                    futures.add(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());
        assets = Futures.transform(assets, assetList ->
                assetList == null ? Collections.emptyList() : assetList.stream().filter(asset -> query.getAssetTypes().contains(asset.getType())).collect(Collectors.toList()), MoreExecutors.directExecutor()
        );
        return assets;
    }

    @NotNull
    @Override
    public ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantAssetTypes = assetDao.findTenantAssetTypesAsync(tenantId.getId());
        return Futures.transform(tenantAssetTypes,
                assetTypes -> {
                    assetTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return assetTypes;
                }, MoreExecutors.directExecutor());
    }

    @NotNull
    @Override
    public Asset assignAssetToEdge(TenantId tenantId, @NotNull AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign asset to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(asset.getTenantId().getId())) {
            throw new DataValidationException("Can't assign asset to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        return asset;
    }

    @Override
    public Asset unassignAssetFromEdge(TenantId tenantId, @NotNull AssetId assetId, EdgeId edgeId) {
        Asset asset = findAssetById(tenantId, assetId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign asset from non-existent edge!");
        }

        checkAssignedEntityViewsToEdge(tenantId, assetId, edgeId);

        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete asset relation. Edge Id: [{}]", assetId, edgeId);
            throw new RuntimeException(e);
        }
        return asset;
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validateString(type, "Incorrect type " + type);
        Validator.validatePageLink(pageLink);
        return assetDao.findAssetsByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    private final PaginatedRemover<TenantId, Asset> tenantAssetsRemover =
            new PaginatedRemover<TenantId, Asset>() {

                @Override
                protected PageData<Asset> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return assetDao.findAssetsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull Asset entity) {
                    deleteAsset(tenantId, new AssetId(entity.getId().getId()));
                }
            };

    private final PaginatedRemover<CustomerId, Asset> customerAssetsUnasigner = new PaginatedRemover<CustomerId, Asset>() {

        @Override
        protected PageData<Asset> findEntities(@NotNull TenantId tenantId, @NotNull CustomerId id, PageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull Asset entity) {
            unassignAssetFromCustomer(tenantId, new AssetId(entity.getId().getId()));
        }
    };
}
