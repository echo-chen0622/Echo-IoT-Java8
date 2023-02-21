package org.echoiot.rule.engine.rabbitmq;

import com.google.common.util.concurrent.ListenableFuture;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.echoiot.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "rabbitmq",
        configClazz = TbRabbitMqNodeConfiguration.class,
        nodeDescription = "Publish messages to the RabbitMQ",
        nodeDetails = "Will publish message payload to RabbitMQ queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeRabbitMqConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIHZlcnNpb249IjEuMSIgeT0iMHB4IiB4PSIwcHgiIHZpZXdCb3g9IjAgMCAxMDAwIDEwMDAiPjxwYXRoIHN0cm9rZS13aWR0aD0iLjg0OTU2IiBkPSJtODYwLjQ3IDQxNi4zMmgtMjYyLjAxYy0xMi45MTMgMC0yMy42MTgtMTAuNzA0LTIzLjYxOC0yMy42MTh2LTI3Mi43MWMwLTIwLjMwNS0xNi4yMjctMzYuMjc2LTM2LjI3Ni0zNi4yNzZoLTkzLjc5MmMtMjAuMzA1IDAtMzYuMjc2IDE2LjIyNy0zNi4yNzYgMzYuMjc2djI3MC44NGMtMC4yNTQ4NyAxNC4xMDMtMTEuNDY5IDI1LjU3Mi0yNS43NDIgMjUuNTcybC04NS42MzYgMC42Nzk2NWMtMTQuMTAzIDAtMjUuNTcyLTExLjQ2OS0yNS41NzItMjUuNTcybDAuNjc5NjUtMjcxLjUyYzAtMjAuMzA1LTE2LjIyNy0zNi4yNzYtMzYuMjc2LTM2LjI3NmgtOTMuNTM3Yy0yMC4zMDUgMC0zNi4yNzYgMTYuMjI3LTM2LjI3NiAzNi4yNzZ2NzYzLjg0YzAgMTguMDk2IDE0Ljc4MiAzMi40NTMgMzIuNDUzIDMyLjQ1M2g3MjIuODFjMTguMDk2IDAgMzIuNDUzLTE0Ljc4MiAzMi40NTMtMzIuNDUzdi00MzUuMzFjLTEuMTg5NC0xOC4xODEtMTUuMjkyLTMyLjE5OC0zMy4zODgtMzIuMTk4em0tMTIyLjY4IDI4Ny4wN2MwIDIzLjYxOC0xOC44NiA0Mi40NzgtNDIuNDc4IDQyLjQ3OGgtNzMuOTk3Yy0yMy42MTggMC00Mi40NzgtMTguODYtNDIuNDc4LTQyLjQ3OHYtNzQuMjUyYzAtMjMuNjE4IDE4Ljg2LTQyLjQ3OCA0Mi40NzgtNDIuNDc4aDczLjk5N2MyMy42MTggMCA0Mi40NzggMTguODYgNDIuNDc4IDQyLjQ3OHoiLz48L3N2Zz4="
)
public class TbRabbitMqNode implements TbNode {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final String ERROR = "error";

    private TbRabbitMqNodeConfiguration config;

    private Connection connection;
    private Channel channel;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRabbitMqNodeConfiguration.class);
        @NotNull ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.config.getHost());
        factory.setPort(this.config.getPort());
        factory.setVirtualHost(this.config.getVirtualHost());
        factory.setUsername(this.config.getUsername());
        factory.setPassword(this.config.getPassword());
        factory.setAutomaticRecoveryEnabled(this.config.isAutomaticRecoveryEnabled());
        factory.setConnectionTimeout(this.config.getConnectionTimeout());
        factory.setHandshakeTimeout(this.config.getHandshakeTimeout());
        this.config.getClientProperties().forEach((k,v) -> factory.getClientProperties().put(k,v));
        try {
            this.connection = factory.newConnection();
            this.channel = this.connection.createChannel();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        withCallback(publishMessageAsync(ctx, msg),
                ctx::tellSuccess,
                t -> {
                    TbMsg next = processException(ctx, msg, t);
                    ctx.tellFailure(next, t);
                });
    }

    private ListenableFuture<TbMsg> publishMessageAsync(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        return ctx.getExternalCallExecutor().executeAsync(() -> publishMessage(ctx, msg));
    }

    @NotNull
    private TbMsg publishMessage(TbContext ctx, @NotNull TbMsg msg) throws Exception {
        String exchangeName = "";
        if (!StringUtils.isEmpty(this.config.getExchangeNamePattern())) {
            exchangeName = TbNodeUtils.processPattern(this.config.getExchangeNamePattern(), msg);
        }
        String routingKey = "";
        if (!StringUtils.isEmpty(this.config.getRoutingKeyPattern())) {
            routingKey = TbNodeUtils.processPattern(this.config.getRoutingKeyPattern(), msg);
        }
        @Nullable AMQP.BasicProperties properties = null;
        if (!StringUtils.isEmpty(this.config.getMessageProperties())) {
            properties = convert(this.config.getMessageProperties());
        }
        channel.basicPublish(
                exchangeName,
                routingKey,
                properties,
                msg.getData().getBytes(UTF8));
        return msg;
    }

    private TbMsg processException(@NotNull TbContext ctx, @NotNull TbMsg origMsg, @NotNull Throwable t) {
        @NotNull TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, t.getClass() + ": " + t.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    @Override
    public void destroy() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                log.error("Failed to close connection during destroy()", e);
            }
        }
    }

    @NotNull
    private static AMQP.BasicProperties convert(@NotNull String name) throws TbNodeException {
        switch (name) {
            case "BASIC":
                return MessageProperties.BASIC;
            case "TEXT_PLAIN":
                return MessageProperties.TEXT_PLAIN;
            case "MINIMAL_BASIC":
                return MessageProperties.MINIMAL_BASIC;
            case "MINIMAL_PERSISTENT_BASIC":
                return MessageProperties.MINIMAL_PERSISTENT_BASIC;
            case "PERSISTENT_BASIC":
                return MessageProperties.PERSISTENT_BASIC;
            case "PERSISTENT_TEXT_PLAIN":
                return MessageProperties.PERSISTENT_TEXT_PLAIN;
            default:
                throw new TbNodeException("Message Properties: '" + name + "' is undefined!");
        }
    }
}
