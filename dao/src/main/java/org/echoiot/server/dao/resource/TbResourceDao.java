package org.echoiot.server.dao.resource;

import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.TenantEntityWithDataDao;

import java.util.List;

public interface TbResourceDao extends Dao<TbResource>, TenantEntityWithDataDao {

    TbResource getResource(TenantId tenantId, ResourceType resourceType, String resourceId);

    PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                ResourceType resourceType,
                                                                PageLink pageLink);

    List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                            ResourceType resourceType,
                                                            String[] objectIds,
                                                            String searchText);
}
