package org.echoiot.server.common.msg.tools;

import lombok.Getter;
import org.echoiot.server.common.data.EntityType;

/**
 * Created by Echo on 22.10.18.
 */
public class TbRateLimitsException extends RuntimeException {
    @Getter
    private final EntityType entityType;

    public TbRateLimitsException(EntityType entityType) {
        super(entityType.name() + " rate limits reached!");
        this.entityType = entityType;
    }
}
