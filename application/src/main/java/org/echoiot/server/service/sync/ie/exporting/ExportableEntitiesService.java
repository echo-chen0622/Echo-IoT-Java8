package org.echoiot.server.service.sync.ie.exporting;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.HasId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;

public interface ExportableEntitiesService {

    <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId);

    <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id);

    <E extends HasId<I>, I extends EntityId> E findEntityById(I id);

    <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name);

    <E extends ExportableEntity<I>, I extends EntityId> PageData<E> findEntitiesByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink);

    <I extends EntityId> I getExternalIdByInternal(I internalId);

    <I extends EntityId> void removeById(TenantId tenantId, I id);

}
