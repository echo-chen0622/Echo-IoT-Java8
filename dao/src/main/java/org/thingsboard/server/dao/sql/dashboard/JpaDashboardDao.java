package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.model.sql.DashboardEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDashboardDao extends JpaAbstractSearchTextDao<DashboardEntity, Dashboard> implements DashboardDao {

    @Autowired
    DashboardRepository dashboardRepository;

    @Override
    protected Class<DashboardEntity> getEntityClass() {
        return DashboardEntity.class;
    }

    @Override
    protected JpaRepository<DashboardEntity, UUID> getRepository() {
        return dashboardRepository;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return dashboardRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Dashboard findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(dashboardRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<Dashboard> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public DashboardId getExternalIdByInternal(DashboardId internalId) {
        return Optional.ofNullable(dashboardRepository.getExternalIdById(internalId.getId()))
                .map(DashboardId::new).orElse(null);
    }

    @Override
    public List<Dashboard> findByTenantIdAndTitle(UUID tenantId, String title) {
        return DaoUtil.convertDataList(dashboardRepository.findByTenantIdAndTitle(tenantId, title));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

}
