package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
public interface EntityViewDao extends Dao<EntityView>, ExportableEntityDao<EntityViewId, EntityView> {

    /**
     * Find entity view info by id.
     *
     * @param tenantId the tenant id
     * @param assetId the asset id
     * @return the entity view info object
     */
    EntityViewInfo findEntityViewInfoById(TenantId tenantId, UUID entityViewId);

    /**
     * Save or update device object
     *
     * @param entityView the entity-view object
     * @return saved entity-view object
     */
    EntityView save(TenantId tenantId, EntityView entityView);

    /**
     * Find entity views by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity view infos by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity views by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity view infos by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity views by tenantId and entity view name.
     *
     * @param tenantId the tenantId
     * @param name the entity view name
     * @return the optional entity view object
     */
    Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find entity views by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                UUID customerId,
                                                                PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find entity views by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId,
                                                                       UUID customerId,
                                                                       String type,
                                                                       PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * Find tenants entity view types.
     *
     * @return the list of tenant entity view type objects
     */
    ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId);

    /**
     * Find entity views by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(UUID tenantId,
                                                            UUID edgeId,
                                                            PageLink pageLink);

    /**
     * Find entity views by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(UUID tenantId,
                                                            UUID edgeId,
                                                            String type,
                                                            PageLink pageLink);

}
