package org.thingsboard.server.dao.dashboard;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface DashboardDao.
 */
public interface DashboardDao extends Dao<Dashboard>, TenantEntityDao, ExportableEntityDao<DashboardId, Dashboard> {

    /**
     * Save or update dashboard object
     *
     * @param dashboard the dashboard object
     * @return saved dashboard object
     */
    Dashboard save(TenantId tenantId, Dashboard dashboard);

    List<Dashboard> findByTenantIdAndTitle(UUID tenantId, String title);

}
