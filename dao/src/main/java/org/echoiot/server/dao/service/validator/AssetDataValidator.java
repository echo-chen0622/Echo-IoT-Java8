package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.asset.AssetDao;
import org.echoiot.server.dao.asset.BaseAssetService;
import org.echoiot.server.dao.customer.CustomerDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AssetDataValidator extends DataValidator<Asset> {

    @Resource
    private AssetDao assetDao;

    @Resource
    @Lazy
    private TenantService tenantService;

    @Resource
    private CustomerDao customerDao;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, @NotNull Asset asset) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        if (!BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
            long maxAssets = profileConfiguration.getMaxAssets();
            validateNumberOfEntitiesPerTenant(tenantId, assetDao, maxAssets, EntityType.ASSET);
        }
    }

    @NotNull
    @Override
    protected Asset validateUpdate(TenantId tenantId, @NotNull Asset asset) {
        Asset old = assetDao.findById(asset.getTenantId(), asset.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull Asset asset) {
        if (StringUtils.isEmpty(asset.getName())) {
            throw new DataValidationException("Asset name should be specified!");
        }
        if (asset.getTenantId() == null) {
            throw new DataValidationException("Asset should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(asset.getTenantId())) {
                throw new DataValidationException("Asset is referencing to non-existent tenant!");
            }
        }
        if (asset.getCustomerId() == null) {
            asset.setCustomerId(new CustomerId(ModelConstants.NULL_UUID));
        } else if (!asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign asset to non-existent customer!");
            }
            if (!customer.getTenantId().equals(asset.getTenantId())) {
                throw new DataValidationException("Can't assign asset to customer from different tenant!");
            }
        }
    }
}
