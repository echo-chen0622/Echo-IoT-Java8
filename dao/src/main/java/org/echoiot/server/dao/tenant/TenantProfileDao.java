package org.echoiot.server.dao.tenant;

import org.echoiot.server.common.data.EntityInfo;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.Dao;

import java.util.UUID;

public interface TenantProfileDao extends Dao<TenantProfile> {

    EntityInfo findTenantProfileInfoById(TenantId tenantId, UUID tenantProfileId);

    TenantProfile save(TenantId tenantId, TenantProfile tenantProfile);

    PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink);

    PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink);

    TenantProfile findDefaultTenantProfile(TenantId tenantId);

    EntityInfo findDefaultTenantProfileInfo(TenantId tenantId);

}
