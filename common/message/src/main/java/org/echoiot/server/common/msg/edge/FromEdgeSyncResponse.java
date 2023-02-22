package org.echoiot.server.common.msg.edge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class FromEdgeSyncResponse implements EdgeSessionMsg {

    private final UUID id;
    private final TenantId tenantId;
    private final EdgeId edgeId;
    private final boolean success;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG;
    }
}
