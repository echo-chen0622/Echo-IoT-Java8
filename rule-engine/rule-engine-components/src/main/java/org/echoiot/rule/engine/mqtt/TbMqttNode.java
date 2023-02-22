package org.echoiot.rule.engine.mqtt;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.mqtt.MqttClient;
import org.echoiot.mqtt.MqttClientConfig;
import org.echoiot.mqtt.MqttConnectResult;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.rule.engine.credentials.BasicCredentials;
import org.echoiot.rule.engine.credentials.ClientCredentials;
import org.echoiot.rule.engine.credentials.CredentialsType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "mqtt",
        configClazz = TbMqttNodeConfiguration.class,
        nodeDescription = "Publish messages to the MQTT broker",
        nodeDetails = "Will publish message payload to the MQTT broker with QoS <b>AT_LEAST_ONCE</b>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMqttConfig",
        icon = "call_split"
)
public class TbMqttNode implements TbNode {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final String ERROR = "error";

    protected TbMqttNodeConfiguration mqttNodeConfiguration;

    protected MqttClient mqttClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        try {
            this.mqttNodeConfiguration = TbNodeUtils.convert(configuration, TbMqttNodeConfiguration.class);
            this.mqttClient = initClient(ctx);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        String topic = TbNodeUtils.processPattern(this.mqttNodeConfiguration.getTopicPattern(), msg);
        this.mqttClient.publish(topic, Unpooled.wrappedBuffer(msg.getData().getBytes(UTF8)), MqttQoS.AT_LEAST_ONCE, mqttNodeConfiguration.isRetainedMessage())
                .addListener(future -> {
                            if (future.isSuccess()) {
                                ctx.tellSuccess(msg);
                            } else {
                                TbMsg next = processException(ctx, msg, future.cause());
                                ctx.tellFailure(next, future.cause());
                            }
                        }
                );
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Throwable e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    @Override
    public void destroy() {
        if (this.mqttClient != null) {
            this.mqttClient.disconnect();
        }
    }

    protected MqttClient initClient(TbContext ctx) throws Exception {
        MqttClientConfig config = new MqttClientConfig(getSslContext());
        if (!StringUtils.isEmpty(this.mqttNodeConfiguration.getClientId())) {
            config.setClientId(this.mqttNodeConfiguration.isAppendClientIdSuffix() ?
                    this.mqttNodeConfiguration.getClientId() + "_" + ctx.getServiceId() : this.mqttNodeConfiguration.getClientId());
        }
        config.setCleanSession(this.mqttNodeConfiguration.isCleanSession());

        prepareMqttClientConfig(config);
        MqttClient client = MqttClient.create(config, null);
        client.setEventLoop(ctx.getSharedEventLoop());
        Future<MqttConnectResult> connectFuture = client.connect(this.mqttNodeConfiguration.getHost(), this.mqttNodeConfiguration.getPort());
        MqttConnectResult result;
        try {
            result = connectFuture.get(this.mqttNodeConfiguration.getConnectTimeoutSec(), TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            String hostPort = this.mqttNodeConfiguration.getHost() + ":" + this.mqttNodeConfiguration.getPort();
            throw new RuntimeException(String.format("Failed to connect to MQTT broker at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }

    protected void prepareMqttClientConfig(MqttClientConfig config) throws SSLException {
        ClientCredentials credentials = this.mqttNodeConfiguration.getCredentials();
        if (credentials.getType() == CredentialsType.BASIC) {
            BasicCredentials basicCredentials = (BasicCredentials) credentials;
            config.setUsername(basicCredentials.getUsername());
            config.setPassword(basicCredentials.getPassword());
        }
    }

    @Nullable
    private SslContext getSslContext() throws SSLException {
        return this.mqttNodeConfiguration.isSsl() ? this.mqttNodeConfiguration.getCredentials().initSslContext() : null;
    }

}
