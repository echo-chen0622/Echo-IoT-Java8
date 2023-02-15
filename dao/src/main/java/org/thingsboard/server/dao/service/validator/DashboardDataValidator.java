package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.dashboard.DashboardDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

@Component
public class DashboardDataValidator extends DataValidator<Dashboard> {

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, Dashboard data) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxDashboards = profileConfiguration.getMaxDashboards();
        validateNumberOfEntitiesPerTenant(tenantId, dashboardDao, maxDashboards, EntityType.DASHBOARD);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Dashboard dashboard) {
        if (StringUtils.isEmpty(dashboard.getTitle())) {
            throw new DataValidationException("Dashboard title should be specified!");
        }
        if (dashboard.getTenantId() == null) {
            throw new DataValidationException("Dashboard should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(dashboard.getTenantId())) {
                throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
            }
        }
    }
}
