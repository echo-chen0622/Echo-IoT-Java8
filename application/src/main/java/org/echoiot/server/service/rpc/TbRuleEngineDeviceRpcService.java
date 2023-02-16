package org.echoiot.server.service.rpc;

import org.echoiot.rule.engine.api.RuleEngineRpcService;
import org.echoiot.server.common.msg.rpc.FromDeviceRpcResponse;

/**
 * Created by Echo on 16.04.18.
 */
public interface TbRuleEngineDeviceRpcService extends RuleEngineRpcService {

    /**
     * Handles the RPC response from the Device Actor (Transport).
     *
     * @param response the RPC response
     */
    void processRpcResponseFromDevice(FromDeviceRpcResponse response);
}
