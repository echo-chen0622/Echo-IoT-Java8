package org.thingsboard.server.dao.resource;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface ResourceService {
    TbResource saveResource(TbResource resource);

    TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceId);

    TbResource findResourceById(TenantId tenantId, TbResourceId resourceId);

    TbResourceInfo findResourceInfoById(TenantId tenantId, TbResourceId resourceId);

    ListenableFuture<TbResourceInfo> findResourceInfoByIdAsync(TenantId tenantId, TbResourceId resourceId);

    PageData<TbResourceInfo> findAllTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<TbResourceInfo> findTenantResourcesByTenantId(TenantId tenantId, PageLink pageLink);

    List<TbResource> findTenantResourcesByResourceTypeAndObjectIds(TenantId tenantId, ResourceType lwm2mModel, String[] objectIds);

    PageData<TbResource> findTenantResourcesByResourceTypeAndPageLink(TenantId tenantId, ResourceType lwm2mModel, PageLink pageLink);

    void deleteResource(TenantId tenantId, TbResourceId resourceId);

    void deleteResourcesByTenantId(TenantId tenantId);

    long sumDataSizeByTenantId(TenantId tenantId);
}
