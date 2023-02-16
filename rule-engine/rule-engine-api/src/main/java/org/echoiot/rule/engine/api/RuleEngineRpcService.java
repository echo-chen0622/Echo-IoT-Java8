package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.id.RpcId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rpc.Rpc;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineRpcService {

    void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body);

    void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest request, Consumer<RuleEngineDeviceRpcResponse> consumer);

    Rpc findRpcById(TenantId tenantId, RpcId id);
}
