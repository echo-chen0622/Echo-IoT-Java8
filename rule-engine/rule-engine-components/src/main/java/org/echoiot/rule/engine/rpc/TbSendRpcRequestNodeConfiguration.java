package org.echoiot.rule.engine.rpc;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbSendRpcRequestNodeConfiguration implements NodeConfiguration<TbSendRpcRequestNodeConfiguration> {

    private int timeoutInSeconds;

    @NotNull
    @Override
    public TbSendRpcRequestNodeConfiguration defaultConfiguration() {
        @NotNull TbSendRpcRequestNodeConfiguration configuration = new TbSendRpcRequestNodeConfiguration();
        configuration.setTimeoutInSeconds(60);
        return configuration;
    }
}
