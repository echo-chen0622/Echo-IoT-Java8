package org.echoiot.server.actors.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@Getter
@ToString
public final class StatsPersistMsg implements TbActorMsg {

    private final long messagesProcessed;
    private final long errorsOccurred;
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final EntityId entityId;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.STATS_PERSIST_MSG;
    }

    public boolean isEmpty() {
        return messagesProcessed == 0 && errorsOccurred == 0;
    }

}
