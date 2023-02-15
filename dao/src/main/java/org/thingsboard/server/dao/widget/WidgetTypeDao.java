package org.thingsboard.server.dao.widget;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

/**
 * The Interface WidgetTypeDao.
 */
public interface WidgetTypeDao extends Dao<WidgetTypeDetails> {

    /**
     * Save or update widget type object
     *
     * @param widgetTypeDetails the widget type details object
     * @return saved widget type object
     */
    WidgetTypeDetails save(TenantId tenantId, WidgetTypeDetails widgetTypeDetails);

    /**
     * Find widget type by tenantId and widgetTypeId.
     *
     * @param tenantId the tenantId
     * @param widgetTypeId the widget type id
     * @return the widget type object
     */
    WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId);

    /**
     * Find widget types by tenantId and bundleAlias.
     *
     * @param tenantId the tenantId
     * @param bundleAlias the bundle alias
     * @return the list of widget types objects
     */
    List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    /**
     * Find widget types details by tenantId and bundleAlias.
     *
     * @param tenantId the tenantId
     * @param bundleAlias the bundle alias
     * @return the list of widget types details objects
     */
    List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    /**
     * Find widget types infos by tenantId and bundleAlias.
     *
     * @param tenantId the tenantId
     * @param bundleAlias the bundle alias
     * @return the list of widget types infos objects
     */
    List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    /**
     * Find widget type by tenantId, bundleAlias and alias.
     *
     * @param tenantId the tenantId
     * @param bundleAlias the bundle alias
     * @param alias the alias
     * @return the widget type object
     */
    WidgetType findByTenantIdBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias);

}
