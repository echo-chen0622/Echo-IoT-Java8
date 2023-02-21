package org.echoiot.server.dao.sql.tenant;

import org.echoiot.server.common.data.EntityInfo;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.TenantProfileEntity;
import org.echoiot.server.dao.sql.JpaAbstractSearchTextDao;
import org.echoiot.server.dao.tenant.TenantProfileDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;

@Component
@SqlDao
public class JpaTenantProfileDao extends JpaAbstractSearchTextDao<TenantProfileEntity, TenantProfile> implements TenantProfileDao {

    @Resource
    private TenantProfileRepository tenantProfileRepository;

    @NotNull
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

    @NotNull
    @Override
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(
                tenantProfileRepository.findTenantProfiles(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @NotNull
    @Override
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, @NotNull PageLink pageLink) {
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
