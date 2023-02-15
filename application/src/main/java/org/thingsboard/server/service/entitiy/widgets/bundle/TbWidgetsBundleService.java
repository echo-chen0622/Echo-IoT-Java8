package org.thingsboard.server.service.entitiy.widgets.bundle;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

public interface TbWidgetsBundleService {

    WidgetsBundle save(WidgetsBundle entity, User currentUser) throws Exception;

    void delete(WidgetsBundle entity) throws ThingsboardException;
}
