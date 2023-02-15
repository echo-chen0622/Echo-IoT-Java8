package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;

@Component
public class ResourceDataValidator extends DataValidator<TbResource> {

    @Autowired
    private TbResourceDao resourceDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, TbResource resource) {
        if (tenantId != null && !TenantId.SYS_TENANT_ID.equals(tenantId)) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxSumResourcesDataInBytes = profileConfiguration.getMaxResourcesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, resourceDao, maxSumResourcesDataInBytes, resource.getData().length(), TB_RESOURCE);
        }
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, TbResource resource) {
        if (StringUtils.isEmpty(resource.getTitle())) {
            throw new DataValidationException("Resource title should be specified!");
        }
        if (resource.getResourceType() == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (StringUtils.isEmpty(resource.getFileName())) {
            throw new DataValidationException("Resource file name should be specified!");
        }
        if (StringUtils.isEmpty(resource.getResourceKey())) {
            throw new DataValidationException("Resource key should be specified!");
        }
        if (resource.getTenantId() == null) {
            resource.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!resource.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(resource.getTenantId())) {
                throw new DataValidationException("Resource is referencing to non-existent tenant!");
            }
        }
    }
}
