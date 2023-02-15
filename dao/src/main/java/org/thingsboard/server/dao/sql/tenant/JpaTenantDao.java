package org.thingsboard.server.dao.sql.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TenantEntity;
import org.thingsboard.server.dao.model.sql.TenantInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Component
@SqlDao
public class JpaTenantDao extends JpaAbstractSearchTextDao<TenantEntity, Tenant> implements TenantDao {

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    @Override
    protected JpaRepository<TenantEntity, UUID> getRepository() {
        return tenantRepository;
    }

    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId, UUID id) {
        return DaoUtil.getData(tenantRepository.findTenantInfoById(id));
    }

    @Override
    public PageData<Tenant> findTenants(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantsNextPage(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<TenantInfo> findTenantInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(tenantRepository
                .findTenantInfosNextPage(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, TenantInfoEntity.tenantInfoColumnMap)));
    }

    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(tenantRepository.findTenantsIds(DaoUtil.toPageable(pageLink))).mapData(TenantId::fromUUID);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        return tenantRepository.findTenantIdsByTenantProfileId(tenantProfileId.getId()).stream()
                .map(TenantId::fromUUID)
                .collect(Collectors.toList());
    }
}
