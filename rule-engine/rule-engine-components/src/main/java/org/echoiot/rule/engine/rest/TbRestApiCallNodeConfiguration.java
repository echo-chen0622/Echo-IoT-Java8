package org.echoiot.rule.engine.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.rule.engine.credentials.AnonymousCredentials;
import org.echoiot.rule.engine.credentials.ClientCredentials;

import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TbRestApiCallNodeConfiguration implements NodeConfiguration<TbRestApiCallNodeConfiguration> {

    private String restEndpointUrlPattern;
    private String requestMethod;
    private Map<String, String> headers;
    private boolean useSimpleClientHttpFactory;
    private int readTimeoutMs;
    private int maxParallelRequestsCount;
    private boolean useRedisQueueForMsgPersistence;
    private boolean trimQueue;
    private int maxQueueSize;
    private boolean enableProxy;
    private boolean useSystemProxyProperties;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private String proxyScheme;
    private ClientCredentials credentials;
    private boolean ignoreRequestBody;

    @NotNull
    @Override
    public TbRestApiCallNodeConfiguration defaultConfiguration() {
        @NotNull TbRestApiCallNodeConfiguration configuration = new TbRestApiCallNodeConfiguration();
        configuration.setRestEndpointUrlPattern("http://localhost/api");
        configuration.setRequestMethod("POST");
        configuration.setHeaders(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        configuration.setUseSimpleClientHttpFactory(false);
        configuration.setReadTimeoutMs(0);
        configuration.setMaxParallelRequestsCount(0);
        configuration.setUseRedisQueueForMsgPersistence(false);
        configuration.setTrimQueue(false);
        configuration.setEnableProxy(false);
        configuration.setCredentials(new AnonymousCredentials());
        configuration.setIgnoreRequestBody(false);
        return configuration;
    }

    @NotNull
    public ClientCredentials getCredentials() {
        if (this.credentials == null) {
            return new AnonymousCredentials();
        } else {
            return this.credentials;
        }
    }
}
