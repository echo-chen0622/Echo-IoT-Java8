package org.thingsboard.server.dao.sql.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Component
@SqlDao
public class JpaTenantProfileDao extends JpaAbstractSearchTextDao<TenantProfileEntity, TenantProfile> implements TenantProfileDao {

    @Autowired
    private TenantProfileRepository tenantProfileRepository;

    @Override
    protected Class<TenantProfileEntity> getEntityClass() {
        return TenantProfileEntity.class;
    }

    @Override
    protected JpaRepository<TenantProfileEntity, UUID> getRepository() {
        return tenantProfileRepository;
    }

    @Override
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, UUID tenantProfileId) {
        return tenantProfileRepository.findTenantProfileInfoById(tenantProfileId);
    }

    @Override
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                tenantProfileRepository.findTenantProfiles(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                tenantProfileRepository.findTenantProfileInfos(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        return DaoUtil.getData(tenantProfileRepository.findByDefaultTrue());
    }

    @Override
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        return tenantProfileRepository.findDefaultTenantProfileInfo();
    }
}
