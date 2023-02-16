package org.echoiot.server.dao.tenant;

import org.echoiot.server.common.data.EntityInfo;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

public interface TenantProfileService {

    TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId);

    EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId);

    TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile);

    void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId);

    PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink);

    PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink);

    TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId);

    TenantProfile findDefaultTenantProfile(TenantId tenantId);

    EntityInfo findDefaultTenantProfileInfo(TenantId tenantId);

    boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId);

    void deleteTenantProfiles(TenantId tenantId);

}
