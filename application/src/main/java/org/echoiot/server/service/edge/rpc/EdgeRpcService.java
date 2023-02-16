package org.echoiot.server.service.edge.rpc;

import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.edge.EdgeSessionMsg;
import org.echoiot.server.common.msg.edge.FromEdgeSyncResponse;
import org.echoiot.server.common.msg.edge.ToEdgeSyncRequest;

import java.util.function.Consumer;

public interface EdgeRpcService {

    void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg);

    void updateEdge(TenantId tenantId, Edge edge);

    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    void processSyncRequest(ToEdgeSyncRequest request, Consumer<FromEdgeSyncResponse> responseConsumer);
}
