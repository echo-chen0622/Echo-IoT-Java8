package org.echoiot.rule.engine.filter;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Echo on 19.01.18.
 */
@Data
public class TbMsgTypeFilterNodeConfiguration implements NodeConfiguration<TbMsgTypeFilterNodeConfiguration> {

    private List<String> messageTypes;

    @NotNull
    @Override
    public TbMsgTypeFilterNodeConfiguration defaultConfiguration() {
        @NotNull TbMsgTypeFilterNodeConfiguration configuration = new TbMsgTypeFilterNodeConfiguration();
        configuration.setMessageTypes(Arrays.asList(
                SessionMsgType.POST_ATTRIBUTES_REQUEST.name(),
                SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                SessionMsgType.TO_SERVER_RPC_REQUEST.name()));
        return configuration;
    }
}
