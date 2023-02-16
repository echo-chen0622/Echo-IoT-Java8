package org.echoiot.server.service.entitiy.widgets.bundle;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Service;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;

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
