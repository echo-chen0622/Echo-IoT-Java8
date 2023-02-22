package org.echoiot.rule.engine.rpc;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

@Data
public class TbSendRpcRequestNodeConfiguration implements NodeConfiguration<TbSendRpcRequestNodeConfiguration> {

    private int timeoutInSeconds;

    @Override
    public TbSendRpcRequestNodeConfiguration defaultConfiguration() {
        TbSendRpcRequestNodeConfiguration configuration = new TbSendRpcRequestNodeConfiguration();
        configuration.setTimeoutInSeconds(60);
        return configuration;
    }
}
