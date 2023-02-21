package org.echoiot.server.service.entitiy.widgets.bundle;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultWidgetsBundleService extends AbstractTbEntityService implements TbWidgetsBundleService {

    @NotNull
    private final WidgetsBundleService widgetsBundleService;

    @NotNull
    @Override
    public WidgetsBundle save(@NotNull WidgetsBundle widgetsBundle, User user) throws Exception {
        WidgetsBundle savedWidgetsBundle = checkNotNull(widgetsBundleService.saveWidgetsBundle(widgetsBundle));
        autoCommit(user, savedWidgetsBundle.getId());
        notificationEntityService.notifySendMsgToEdgeService(widgetsBundle.getTenantId(), savedWidgetsBundle.getId(),
                widgetsBundle.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);
        return savedWidgetsBundle;
    }

    @Override
    public void delete(@NotNull WidgetsBundle widgetsBundle) throws EchoiotException {
        widgetsBundleService.deleteWidgetsBundle(widgetsBundle.getTenantId(), widgetsBundle.getId());
        notificationEntityService.notifySendMsgToEdgeService(widgetsBundle.getTenantId(), widgetsBundle.getId(),
                EdgeEventActionType.DELETED);
    }
}
