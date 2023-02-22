package org.echoiot.server.dao.sql.asset;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetProfileInfo;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.asset.AssetProfileDao;
import org.echoiot.server.dao.model.sql.AssetProfileEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAssetProfileDao extends JpaAbstractSearchTextDao<AssetProfileEntity, AssetProfile> implements AssetProfileDao {

    @Resource
    private AssetProfileRepository assetProfileRepository;

    @Override
    protected Class<AssetProfileEntity> getEntityClass() {
        return AssetProfileEntity.class;
    }

    @Override
    protected JpaRepository<AssetProfileEntity, UUID> getRepository() {
        return assetProfileRepository;
    }

    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId) {
        return assetProfileRepository.findAssetProfileInfoById(assetProfileId);
    }

    @Transactional
    @Override
    public AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfile result = save(tenantId, assetProfile);
        assetProfileRepository.flush();
        return result;
    }

    @Override
    public PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetProfileRepository.findAssetProfiles(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                assetProfileRepository.findAssetProfileInfos(
                        tenantId.getId(),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        return DaoUtil.getData(assetProfileRepository.findByDefaultTrueAndTenantId(tenantId.getId()));
    }

    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        return assetProfileRepository.findDefaultAssetProfileInfo(tenantId.getId());
    }

    @Override
    public AssetProfile findByName(TenantId tenantId, String profileName) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId.getId(), profileName));
    }

    @Override
    public AssetProfile findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public AssetProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<AssetProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetProfiles(TenantId.fromUUID(tenantId), pageLink);
    }

    @Nullable
    @Override
    public AssetProfileId getExternalIdByInternal(AssetProfileId internalId) {
        return Optional.ofNullable(assetProfileRepository.getExternalIdById(internalId.getId()))
                .map(AssetProfileId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
