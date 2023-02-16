package org.echoiot.server.service.edge;

import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.queue.TbCallback;
import org.echoiot.server.gen.transport.TransportProtos;

public interface EdgeNotificationService {

    Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws Exception;

    void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback);
}
