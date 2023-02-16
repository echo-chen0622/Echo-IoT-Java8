package org.echoiot.server.service.entitiy.widgets.bundle;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.widget.WidgetsBundle;

public interface TbWidgetsBundleService {

    WidgetsBundle save(WidgetsBundle entity, User currentUser) throws Exception;

    void delete(WidgetsBundle entity) throws ThingsboardException;
}
