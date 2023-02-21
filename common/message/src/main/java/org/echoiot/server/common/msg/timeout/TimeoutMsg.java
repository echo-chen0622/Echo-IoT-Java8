package org.echoiot.server.common.msg.timeout;

import lombok.Data;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

/**
 * @author Andrew Shvayka
 */
@Data
public abstract class TimeoutMsg<T> implements TbActorMsg {
    @NotNull
    private final T id;
    private final long timeout;
}
