package org.thingsboard.server.service.update;

import org.thingsboard.server.common.data.UpdateMessage;

public interface UpdateService {

    UpdateMessage checkUpdates();

}
