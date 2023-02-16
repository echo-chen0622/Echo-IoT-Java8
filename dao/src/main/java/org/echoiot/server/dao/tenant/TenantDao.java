package org.echoiot.server.dao.tenant;

import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface TenantDao extends Dao<Tenant> {

    TenantInfo findTenantInfoById(TenantId tenantId, UUID id);

    /**
     * Save or update tenant object
     *
     * @param tenant the tenant object
     * @return saved tenant object
     */
    Tenant save(TenantId tenantId, Tenant tenant);

    /**
     * Find tenants by page link.
     *
     * @param pageLink the page link
     * @return the list of tenant objects
     */
    PageData<Tenant> findTenants(TenantId tenantId, PageLink pageLink);

    PageData<TenantInfo> findTenantInfos(TenantId tenantId, PageLink pageLink);

    PageData<TenantId> findTenantsIds(PageLink pageLink);

    List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId);
}
