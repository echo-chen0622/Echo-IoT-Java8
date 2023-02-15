package org.thingsboard.server.service.edge;

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

public interface EdgeNotificationService {

    Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws Exception;

    void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback);
}
