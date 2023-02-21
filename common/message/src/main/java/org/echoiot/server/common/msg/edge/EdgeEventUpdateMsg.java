package org.echoiot.server.common.msg.edge;

import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.MsgType;
import org.jetbrains.annotations.NotNull;

@ToString
public class EdgeEventUpdateMsg implements EdgeSessionMsg {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EdgeId edgeId;

    public EdgeEventUpdateMsg(TenantId tenantId, EdgeId edgeId) {
        this.tenantId = tenantId;
        this.edgeId = edgeId;
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG;
    }
}
