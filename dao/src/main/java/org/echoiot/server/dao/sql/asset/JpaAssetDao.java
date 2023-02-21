package org.echoiot.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetInfo;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.common.data.util.TbPair;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.asset.AssetDao;
import org.echoiot.server.dao.model.sql.AssetEntity;
import org.echoiot.server.dao.model.sql.AssetInfoEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static org.echoiot.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Component
@SqlDao
@Slf4j
public class JpaAssetDao extends JpaAbstractSearchTextDao<AssetEntity, Asset> implements AssetDao {

    @Resource
    private AssetRepository assetRepository;

    @NotNull
    @Override
    protected Class<AssetEntity> getEntityClass() {
        return AssetEntity.class;
    }

    @Override
    protected JpaRepository<AssetEntity, UUID> getRepository() {
        return assetRepository;
    }

    @Override
    public AssetInfo findAssetInfoById(TenantId tenantId, UUID assetId) {
        return DaoUtil.getData(assetRepository.findAssetInfoById(assetId));
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndIdIn(tenantId, assetIds)));
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, assetIds)));
    }

    @NotNull
    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(asset);
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndType(UUID tenantId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndType(
                        tenantId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndAssetProfileId(UUID tenantId, UUID assetProfileId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndAssetProfileId(
                        tenantId,
                        assetProfileId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @NotNull
    @Override
    public PageData<AssetInfo> findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(UUID tenantId, UUID customerId, UUID assetProfileId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(
                        tenantId,
                        customerId,
                        assetProfileId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, AssetInfoEntity.assetInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantAssetTypesToDto(tenantId, assetRepository.findTenantAssetTypes(tenantId)));
    }

    @Override
    public Long countAssetsByAssetProfileId(TenantId tenantId, UUID assetProfileId) {
        return assetRepository.countByAssetProfileId(assetProfileId);
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndProfileId(UUID tenantId, UUID profileId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                assetRepository.findByTenantIdAndProfileId(
                        tenantId,
                        profileId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    private List<EntitySubtype> convertTenantAssetTypesToDto(UUID tenantId, @Nullable List<String> types) {
        @NotNull List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(TenantId.fromUUID(tenantId), EntityType.ASSET, type));
            }
        }
        return list;
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, @NotNull PageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<Asset> findAssetsByTenantIdAndEdgeIdAndType(UUID tenantId, UUID edgeId, String type, @NotNull PageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], edgeId [{}], type [{}] and pageLink [{}]", tenantId, edgeId, type, pageLink);
        return DaoUtil.toPageData(assetRepository
                .findByTenantIdAndEdgeIdAndType(
                        tenantId,
                        edgeId,
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    public PageData<TbPair<UUID, String>> getAllAssetTypes(PageLink pageLink) {
        log.debug("Try to find all asset types and pageLink [{}]", pageLink);
        return DaoUtil.pageToPageData(assetRepository.getAllAssetTypes(
                DaoUtil.toPageable(pageLink, Arrays.asList(new SortOrder("tenantId"), new SortOrder("type")))));
    }

    @Override
    public Long countByTenantId(@NotNull TenantId tenantId) {
        return assetRepository.countByTenantIdAndTypeIsNot(tenantId.getId(), TB_SERVICE_QUEUE);
    }

    @Override
    public Asset findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Nullable
    @Override
    public Asset findByTenantIdAndName(UUID tenantId, String name) {
        return findAssetsByTenantIdAndName(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Asset> findByTenantId(UUID tenantId, @NotNull PageLink pageLink) {
        return findAssetsByTenantId(tenantId, pageLink);
    }

    @Nullable
    @Override
    public AssetId getExternalIdByInternal(@NotNull AssetId internalId) {
        return Optional.ofNullable(assetRepository.getExternalIdById(internalId.getId()))
                .map(AssetId::new).orElse(null);
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
