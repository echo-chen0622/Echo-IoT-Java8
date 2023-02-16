package org.echoiot.server.service.sync.ie.importing.impl;

import lombok.Getter;
import org.echoiot.server.common.data.id.EntityId;

public class MissingEntityException extends ImportServiceException {

    private static final long serialVersionUID = 3669135386955906022L;
    @Getter
    private final EntityId entityId;

    public MissingEntityException(EntityId entityId) {
        this.entityId = entityId;
    }
}
