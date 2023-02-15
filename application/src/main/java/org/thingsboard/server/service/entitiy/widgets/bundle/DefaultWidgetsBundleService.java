package org.thingsboard.server.service.entitiy.widgets.bundle;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetsBundleService extends AbstractTbEntityService implements TbWidgetsBundleService {

    private final WidgetsBundleService widgetsBundleService;

    @Override
    public WidgetsBundle save(WidgetsBundle widgetsBundle, User user) throws Exception {
        WidgetsBundle savedWidgetsBundle = checkNotNull(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        autoCommit(user, savedWidgetsBundle.getId());
        notificationEntityService.notifySendMsgToEdgeService(widgetsBundle.getTenantId(), savedWidgetsBundle.getId(),
                widgetsBundle.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
        return savedWidgetsBundle;
    }

    @Override
    public void delete(WidgetsBundle widgetsBundle) throws ThingsboardException {
        widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getTenantId(), widgetsBundle.getId());
        notificationEntityService.notifySendMsgToEdgeService(widgetsBundle.getTenantId(), widgetsBundle.getId(),
                EdgeEventActionType.DELETED);
    }
}
