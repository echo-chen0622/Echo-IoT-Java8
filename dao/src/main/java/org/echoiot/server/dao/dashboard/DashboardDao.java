package org.echoiot.server.dao.dashboard;

import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.ExportableEntityDao;
import org.echoiot.server.dao.TenantEntityDao;

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
