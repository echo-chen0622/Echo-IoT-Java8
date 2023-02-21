package org.echoiot.server.common.msg.tools;

import lombok.Getter;
import org.echoiot.server.common.data.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 22.10.18.
 */
public class TbRateLimitsException extends RuntimeException {
    @NotNull
    @Getter
    private final EntityType entityType;

    public TbRateLimitsException(@NotNull EntityType entityType) {
        super(entityType.name() + " rate limits reached!");
        this.entityType = entityType;
    }
}
