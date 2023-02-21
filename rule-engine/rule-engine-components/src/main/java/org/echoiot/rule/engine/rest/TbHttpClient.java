package org.echoiot.rule.engine.rest;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.echoiot.server.common.data.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.rule.engine.credentials.BasicCredentials;
import org.echoiot.rule.engine.credentials.ClientCredentials;
import org.echoiot.rule.engine.credentials.CredentialsType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Data
@Slf4j
@SuppressWarnings("deprecation")
public class TbHttpClient {

    private static final String STATUS = "status";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_REASON = "statusReason";
    private static final String ERROR = "error";
    private static final String ERROR_BODY = "error_body";
    private static final String ERROR_SYSTEM_PROPERTIES = "Didn't set any system proxy properties. Should be added next system proxy properties: \"http.proxyHost\" and \"http.proxyPort\" or  \"https.proxyHost\" and \"https.proxyPort\" or \"socksProxyHost\" and \"socksProxyPort\"";

    private final TbRestApiCallNodeConfiguration config;

    private EventLoopGroup eventLoopGroup;
    private AsyncRestTemplate httpClient;
    private Deque<ListenableFuture<ResponseEntity<String>>> pendingFutures;

    TbHttpClient(@NotNull TbRestApiCallNodeConfiguration config, EventLoopGroup eventLoopGroupShared) throws TbNodeException {
        try {
            this.config = config;
            if (config.getMaxParallelRequestsCount() > 0) {
                pendingFutures = new ConcurrentLinkedDeque<>();
            }

            if (config.isEnableProxy()) {
                checkProxyHost(config.getProxyHost());
                checkProxyPort(config.getProxyPort());

                String proxyUser;
                String proxyPassword;

                CloseableHttpAsyncClient asyncClient;
                @NotNull HttpComponentsAsyncClientHttpRequestFactory requestFactory = new HttpComponentsAsyncClientHttpRequestFactory();

                if (config.isUseSystemProxyProperties()) {
                    checkSystemProxyProperties();

                    asyncClient = HttpAsyncClients.createSystem();

                    proxyUser = System.getProperty("tb.proxy.user");
                    proxyPassword = System.getProperty("tb.proxy.password");

                    if (useAuth(proxyUser, proxyPassword)) {
                        Authenticator.setDefault(new Authenticator() {
                            @NotNull
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                            }
                        });
                    }
                } else {
                    @NotNull HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClientBuilder.create()
                                                                                                   .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                                                                                                   .setSSLContext(SSLContext.getDefault())
                                                                                                   .setProxy(new HttpHost(config.getProxyHost(), config.getProxyPort(), config.getProxyScheme()));

                    proxyUser = config.getProxyUser();
                    proxyPassword = config.getProxyPassword();

                    if (useAuth(proxyUser, proxyPassword)) {
                        @NotNull CredentialsProvider credsProvider = new BasicCredentialsProvider();
                        credsProvider.setCredentials(
                                new AuthScope(config.getProxyHost(), config.getProxyPort()),
                                new UsernamePasswordCredentials(proxyUser, proxyPassword)
                        );
                        httpAsyncClientBuilder.setDefaultCredentialsProvider(credsProvider);
                    }
                    asyncClient = httpAsyncClientBuilder.build();
                }

                requestFactory.setAsyncClient(asyncClient);
                requestFactory.setReadTimeout(config.getReadTimeoutMs());
                httpClient = new AsyncRestTemplate(requestFactory);
            } else if (config.isUseSimpleClientHttpFactory()) {
                if (CredentialsType.CERT_PEM == config.getCredentials().getType()) {
                    throw new TbNodeException("Simple HTTP Factory does not support CERT PEM credentials!");
                }
                httpClient = new AsyncRestTemplate();
            } else {
                @NotNull Netty4ClientHttpRequestFactory nettyFactory = new Netty4ClientHttpRequestFactory(getSharedOrCreateEventLoopGroup(eventLoopGroupShared));
                nettyFactory.setSslContext(config.getCredentials().initSslContext());
                nettyFactory.setReadTimeout(config.getReadTimeoutMs());
                httpClient = new AsyncRestTemplate(nettyFactory);
            }
        } catch (SSLException | NoSuchAlgorithmException e) {
            throw new TbNodeException(e);
        }
    }

    @NotNull
    EventLoopGroup getSharedOrCreateEventLoopGroup(@Nullable EventLoopGroup eventLoopGroupShared) {
        if (eventLoopGroupShared != null) {
            return eventLoopGroupShared;
        }
        return this.eventLoopGroup = new NioEventLoopGroup();
    }

    private void checkSystemProxyProperties() throws TbNodeException {
        boolean useHttpProxy = !StringUtils.isEmpty(System.getProperty("http.proxyHost")) && !StringUtils.isEmpty(System.getProperty("http.proxyPort"));
        boolean useHttpsProxy = !StringUtils.isEmpty(System.getProperty("https.proxyHost")) && !StringUtils.isEmpty(System.getProperty("https.proxyPort"));
        boolean useSocksProxy = !StringUtils.isEmpty(System.getProperty("socksProxyHost")) && !StringUtils.isEmpty(System.getProperty("socksProxyPort"));
        if (!(useHttpProxy || useHttpsProxy || useSocksProxy)) {
            log.warn(ERROR_SYSTEM_PROPERTIES);
            throw new TbNodeException(ERROR_SYSTEM_PROPERTIES);
        }
    }

    private boolean useAuth(String proxyUser, String proxyPassword) {
        return !StringUtils.isEmpty(proxyUser) && !StringUtils.isEmpty(proxyPassword);
    }

    void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    public void processMessage(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        String endpointUrl = TbNodeUtils.processPattern(config.getRestEndpointUrlPattern(), msg);
        @NotNull HttpHeaders headers = prepareHeaders(msg);
        @NotNull HttpMethod method = HttpMethod.valueOf(config.getRequestMethod());
        HttpEntity<String> entity;
        if(HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) ||
            HttpMethod.OPTIONS.equals(method) || HttpMethod.TRACE.equals(method) ||
            config.isIgnoreRequestBody()) {
            entity = new HttpEntity<>(headers);
        } else {
            entity = new HttpEntity<>(msg.getData(), headers);
        }

        @NotNull URI uri = buildEncodedUri(endpointUrl);
        @NotNull ListenableFuture<ResponseEntity<String>> future = httpClient.exchange(
                uri, method, entity, String.class);
        future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                TbMsg next = processException(ctx, msg, throwable);
                ctx.tellFailure(next, throwable);
            }

            @Override
            public void onSuccess(@NotNull ResponseEntity<String> responseEntity) {
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    TbMsg next = processResponse(ctx, msg, responseEntity);
                    ctx.tellSuccess(next);
                } else {
                    TbMsg next = processFailureResponse(ctx, msg, responseEntity);
                    ctx.tellNext(next, TbRelationTypes.FAILURE);
                }
            }
        });
        if (pendingFutures != null) {
            processParallelRequests(future);
        }
    }

    @NotNull
    public URI buildEncodedUri(@NotNull String endpointUrl) {
        if (endpointUrl == null) {
            throw new RuntimeException("Url string cannot be null!");
        }
        if (endpointUrl.isEmpty()) {
            throw new RuntimeException("Url string cannot be empty!");
        }

        @NotNull URI uri = UriComponentsBuilder.fromUriString(endpointUrl).build().encode().toUri();
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            throw new RuntimeException("Transport scheme(protocol) must be provided!");
        }

        boolean authorityNotValid = uri.getAuthority() == null || uri.getAuthority().isEmpty();
        boolean hostNotValid = uri.getHost() == null || uri.getHost().isEmpty();
        if (authorityNotValid || hostNotValid) {
            throw new RuntimeException("Url string is invalid!");
        }

        return uri;
    }

    private TbMsg processResponse(@NotNull TbContext ctx, @NotNull TbMsg origMsg, @NotNull ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        @Nullable String body = response.getBody() == null ? "{}" : response.getBody();
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, body);
    }

    void headersToMetaData(@Nullable Map<String, List<String>> headers, @NotNull BiConsumer<String, String> consumer) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                if (values.size() == 1) {
                    consumer.accept(key, values.get(0));
                } else {
                    consumer.accept(key, JacksonUtil.toString(values));
                }
            }
        });
    }

    private TbMsg processFailureResponse(@NotNull TbContext ctx, @NotNull TbMsg origMsg, @NotNull ResponseEntity<String> response) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(STATUS, response.getStatusCode().name());
        metaData.putValue(STATUS_CODE, response.getStatusCode().value() + "");
        metaData.putValue(STATUS_REASON, response.getStatusCode().getReasonPhrase());
        metaData.putValue(ERROR_BODY, response.getBody());
        headersToMetaData(response.getHeaders(), metaData::putValue);
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(@NotNull TbContext ctx, @NotNull TbMsg origMsg, @NotNull Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        if (e instanceof RestClientResponseException) {
            @NotNull RestClientResponseException restClientResponseException = (RestClientResponseException) e;
            metaData.putValue(STATUS, restClientResponseException.getStatusText());
            metaData.putValue(STATUS_CODE, restClientResponseException.getRawStatusCode() + "");
            metaData.putValue(ERROR_BODY, restClientResponseException.getResponseBodyAsString());
        }
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    @NotNull
    private HttpHeaders prepareHeaders(@NotNull TbMsg msg) {
        @NotNull HttpHeaders headers = new HttpHeaders();
        config.getHeaders().forEach((k, v) -> headers.add(TbNodeUtils.processPattern(k, msg), TbNodeUtils.processPattern(v, msg)));
        ClientCredentials credentials = config.getCredentials();
        if (CredentialsType.BASIC == credentials.getType()) {
            @NotNull BasicCredentials basicCredentials = (BasicCredentials) credentials;
            @NotNull String authString = basicCredentials.getUsername() + ":" + basicCredentials.getPassword();
            @NotNull String encodedAuthString = new String(Base64.encodeBase64(authString.getBytes(StandardCharsets.UTF_8)));
            headers.add("Authorization", "Basic " + encodedAuthString);
        }
        return headers;
    }

    private void processParallelRequests(ListenableFuture<ResponseEntity<String>> future) {
        pendingFutures.add(future);
        if (pendingFutures.size() > config.getMaxParallelRequestsCount()) {
            for (int i = 0; i < config.getMaxParallelRequestsCount(); i++) {
                try {
                    ListenableFuture<ResponseEntity<String>> pendingFuture = pendingFutures.removeFirst();
                    try {
                        pendingFuture.get(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.warn("Timeout during waiting for reply!", e);
                        pendingFuture.cancel(true);
                    }
                } catch (Exception e) {
                    log.warn("Failure during waiting for reply!", e);
                }
            }
        }
    }

    private static void checkProxyHost(String proxyHost) throws TbNodeException {
        if (StringUtils.isEmpty(proxyHost)) {
            throw new TbNodeException("Proxy host can't be empty");
        }
    }

    private static void checkProxyPort(int proxyPort) throws TbNodeException {
        if (proxyPort < 0 || proxyPort > 65535) {
            throw new TbNodeException("Proxy port out of range:" + proxyPort);
        }
    }

}
