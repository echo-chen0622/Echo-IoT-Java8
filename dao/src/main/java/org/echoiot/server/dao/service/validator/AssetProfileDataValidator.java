package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.DashboardInfo;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.dao.asset.AssetProfileDao;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AssetProfileDataValidator extends DataValidator<AssetProfile> {

    @Resource
    private AssetProfileDao assetProfileDao;
    @Resource
    @Lazy
    private AssetProfileService assetProfileService;
    @Resource
    private TenantService tenantService;
    @Lazy
    @Resource
    private QueueService queueService;
    @Resource
    private RuleChainService ruleChainService;
    @Resource
    private DashboardService dashboardService;

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull AssetProfile assetProfile) {
        if (StringUtils.isEmpty(assetProfile.getName())) {
            throw new DataValidationException("Asset profile name should be specified!");
        }
        if (assetProfile.getTenantId() == null) {
            throw new DataValidationException("Asset profile should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(assetProfile.getTenantId())) {
                throw new DataValidationException("Asset profile is referencing to non-existent tenant!");
            }
        }
        if (assetProfile.isDefault()) {
            AssetProfile defaultAssetProfile = assetProfileService.findDefaultAssetProfile(tenantId);
            if (defaultAssetProfile != null && !defaultAssetProfile.getId().equals(assetProfile.getId())) {
                throw new DataValidationException("Another default asset profile is present in scope of current tenant!");
            }
        }
        if (StringUtils.isNotEmpty(assetProfile.getDefaultQueueName())) {
            Queue queue = queueService.findQueueByTenantIdAndName(tenantId, assetProfile.getDefaultQueueName());
            if (queue == null) {
                throw new DataValidationException("Asset profile is referencing to non-existent queue!");
            }
        }

        if (assetProfile.getDefaultRuleChainId() != null) {
            RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, assetProfile.getDefaultRuleChainId());
            if (ruleChain == null) {
                throw new DataValidationException("Can't assign non-existent rule chain!");
            }
            if (!ruleChain.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign rule chain from different tenant!");
            }
        }

        if (assetProfile.getDefaultDashboardId() != null) {
            DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, assetProfile.getDefaultDashboardId());
            if (dashboard == null) {
                throw new DataValidationException("Can't assign non-existent dashboard!");
            }
            if (!dashboard.getTenantId().equals(assetProfile.getTenantId())) {
                throw new DataValidationException("Can't assign dashboard from different tenant!");
            }
        }
    }

    @NotNull
    @Override
    protected AssetProfile validateUpdate(TenantId tenantId, @NotNull AssetProfile assetProfile) {
        AssetProfile old = assetProfileDao.findById(assetProfile.getTenantId(), assetProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset profile!");
        }
        return old;
    }

}
