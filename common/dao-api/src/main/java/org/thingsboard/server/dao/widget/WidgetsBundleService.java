package org.thingsboard.server.dao.widget;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.List;

public interface WidgetsBundleService {

    WidgetsBundle findWidgetsBundleById(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle);

    void deleteWidgetsBundle(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias);

    PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TenantId tenantId, PageLink pageLink);

    List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId);

    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId);

    void deleteWidgetsBundlesByTenantId(TenantId tenantId);

}
