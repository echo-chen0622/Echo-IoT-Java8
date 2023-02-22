package org.echoiot.server.dao.resource;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.TbResourceInfo;
import org.echoiot.server.common.data.id.TbResourceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static org.echoiot.server.dao.device.DeviceServiceImpl.INCORRECT_TENANT_ID;
import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
@AllArgsConstructor
public class BaseResourceService implements ResourceService {

    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    private final TbResourceDao resourceDao;
    private final TbResourceInfoDao resourceInfoDao;
    private final DataValidator<TbResource> resourceValidator;

    @Override
    public TbResource saveResource(TbResource resource) {
        resourceValidator.validate(resource, TbResourceInfo::getTenantId);

        try {
            return resourceDao.save(resource.getTenantId(), resource);
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("resource_unq_key")) {
                String field = ResourceType.LWM2M_MODEL.equals(resource.getResourceType()) ? "resourceKey" : "fileName";
                throw new DataValidationException("Resource with such " + field + " already exists!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceKey) {
        log.trace("Executing getResource [{}] [{}] [{}]", tenantId, resourceType, resourceKey);
        return resourceDao.getResource(tenantId, resourceType, resourceKey);
    }

    @Override
    public TbResource findResourceById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceDao.findById(tenantId, resourceId.getId());
    }

    @Override
    public TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceInfoDao.findById(tenantId, resourceId.getId());
    }

    @Override
    public ListenableFuture<TbResourceInfo> findResourceInfoByIdAsync(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing findResourceInfoById [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        return resourceInfoDao.findByIdAsync(tenantId, resourceId.getId());
    }

    @Override
    public void deleteResource(TenantId tenantId, TbResourceId resourceId) {
        log.trace("Executing deleteResource [{}] [{}]", tenantId, resourceId);
        Validator.validateId(resourceId, INCORRECT_RESOURCE_ID + resourceId);
        resourceDao.removeById(tenantId, resourceId.getId());
    }

    @Override
    public PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findAllTenantResourcesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<TbResourceInfo> findTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantResourcesByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceInfoDao.findTenantResourcesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<TbResource> findTenantResourcesByResourceTypeAndObjectIds(TenantId tenantId, ResourceType resourceType, String[] objectIds) {
        log.trace("Executing findTenantResourcesByResourceTypeAndObjectIds [{}][{}][{}]", tenantId, resourceType, objectIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, objectIds, null);
    }

    @Override
    public PageData<TbResource> findTenantResourcesByResourceTypeAndPageLink(TenantId tenantId, ResourceType resourceType, PageLink pageLink) {
        log.trace("Executing findTenantResourcesByResourceTypeAndPageLink [{}][{}][{}]", tenantId, resourceType, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return resourceDao.findResourcesByTenantIdAndResourceType(tenantId, resourceType, pageLink);
    }

    @Override
    public void deleteResourcesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteResourcesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantResourcesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return resourceDao.sumDataSizeByTenantId(tenantId);
    }

    private final PaginatedRemover<TenantId, TbResource> tenantResourcesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TbResource> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return resourceDao.findAllByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TbResource entity) {
                    deleteResource(tenantId, new TbResourceId(entity.getUuidId()));
                }
            };

    protected Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }
}
