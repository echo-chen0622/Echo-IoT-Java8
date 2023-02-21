package org.echoiot.rule.engine.rpc;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;

@Data
public class TbSendRpcReplyNodeConfiguration implements NodeConfiguration<TbSendRpcReplyNodeConfiguration> {

    public static final String SERVICE_ID = "serviceId";
    public static final String SESSION_ID = "sessionId";
    public static final String REQUEST_ID = "requestId";

    private String serviceIdMetaDataAttribute;
    private String sessionIdMetaDataAttribute;
    private String requestIdMetaDataAttribute;

    @NotNull
    @Override
    public TbSendRpcReplyNodeConfiguration defaultConfiguration() {
        @NotNull TbSendRpcReplyNodeConfiguration configuration = new TbSendRpcReplyNodeConfiguration();
        configuration.setServiceIdMetaDataAttribute(SERVICE_ID);
        configuration.setSessionIdMetaDataAttribute(SESSION_ID);
        configuration.setRequestIdMetaDataAttribute(REQUEST_ID);
        return configuration;
    }

    @NotNull
    public String getServiceIdMetaDataAttribute() {
        return !StringUtils.isEmpty(serviceIdMetaDataAttribute) ? serviceIdMetaDataAttribute : SERVICE_ID;
    }

    @NotNull
    public String getSessionIdMetaDataAttribute() {
        return !StringUtils.isEmpty(sessionIdMetaDataAttribute) ? sessionIdMetaDataAttribute : SESSION_ID;
    }

    @NotNull
    public String getRequestIdMetaDataAttribute() {
        return !StringUtils.isEmpty(requestIdMetaDataAttribute) ? requestIdMetaDataAttribute : REQUEST_ID;
    }
}
