package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.resource.TbResourceDao;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ResourceDataValidator extends DataValidator<TbResource> {

    @Resource
    private TbResourceDao resourceDao;

    @Resource
    private TenantService tenantService;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(@Nullable TenantId tenantId, @NotNull TbResource resource) {
        if (tenantId != null && !TenantId.SYS_TENANT_ID.equals(tenantId)) {
            DefaultTenantProfileConfiguration profileConfiguration =
                    (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
            long maxSumResourcesDataInBytes = profileConfiguration.getMaxResourcesInBytes();
            validateMaxSumDataSizePerTenant(tenantId, resourceDao, maxSumResourcesDataInBytes, resource.getData().length(), EntityType.TB_RESOURCE);
        }
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, @NotNull TbResource resource) {
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
