package org.echoiot.server.common.msg.edge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class FromEdgeSyncResponse implements EdgeSessionMsg {

    @NotNull
    private final UUID id;
    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final EdgeId edgeId;
    private final boolean success;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG;
    }
}
