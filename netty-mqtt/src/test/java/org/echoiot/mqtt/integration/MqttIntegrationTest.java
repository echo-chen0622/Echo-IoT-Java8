package org.echoiot.mqtt.integration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.mqtt.MqttClient;
import org.echoiot.mqtt.MqttClientConfig;
import org.echoiot.mqtt.MqttConnectResult;
import org.echoiot.mqtt.integration.server.MqttServer;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class MqttIntegrationTest {

    static final String MQTT_HOST = "localhost";
    static final int KEEPALIVE_TIMEOUT_SECONDS = 2;
    static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);

    EventLoopGroup eventLoopGroup;
    MqttServer mqttServer;

    MqttClient mqttClient;

    @Before
    public void init() throws Exception {
        this.eventLoopGroup = new NioEventLoopGroup();

        this.mqttServer = new MqttServer();
        this.mqttServer.init();
    }

    @After
    public void destroy() throws InterruptedException {
        if (this.mqttClient != null) {
            this.mqttClient.disconnect();
        }
        if (this.mqttServer != null) {
            this.mqttServer.shutdown();
        }
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void givenActiveMqttClient_whenNoActivityForKeepAliveTimeout_thenDisconnectClient() throws Throwable {
        //given
        this.mqttClient = initClient();

        log.warn("Sending publish messages...");
        @NotNull CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            Future<Void> pubFuture = publishMsg();
            pubFuture.addListener(future -> latch.countDown());
        }

        log.warn("Waiting for messages acknowledgments...");
        boolean awaitResult = latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(awaitResult);

        //when
        @NotNull CountDownLatch keepAliveLatch = new CountDownLatch(1);

        log.warn("Starting idle period...");
        boolean keepaliveAwaitResult = keepAliveLatch.await(5, TimeUnit.SECONDS);
        Assert.assertFalse(keepaliveAwaitResult);

        //then
        List<MqttMessageType> allReceivedEvents = this.mqttServer.getEventsFromClient();
        long pubCount = allReceivedEvents.stream().filter(mqttMessageType -> mqttMessageType == MqttMessageType.PUBLISH).count();
        long disconnectCount = allReceivedEvents.stream().filter(type -> type == MqttMessageType.DISCONNECT).count();

        Assert.assertEquals(3, pubCount);
        Assert.assertEquals(1, disconnectCount);
    }

    private Future<Void> publishMsg() {
        ByteBuf byteBuf = ALLOCATOR.buffer();
        byteBuf.writeBytes("payload".getBytes(StandardCharsets.UTF_8));
        return this.mqttClient.publish(
                "test/topic",
                byteBuf,
                MqttQoS.AT_LEAST_ONCE);
    }

    @NotNull
    private MqttClient initClient() throws Exception {
        @NotNull MqttClientConfig config = new MqttClientConfig();
        config.setTimeoutSeconds(KEEPALIVE_TIMEOUT_SECONDS);
        @NotNull MqttClient client = MqttClient.create(config, null);
        client.setEventLoop(this.eventLoopGroup);
        Future<MqttConnectResult> connectFuture = client.connect(MQTT_HOST, this.mqttServer.getMqttPort());

        @NotNull String hostPort = MQTT_HOST + ":" + this.mqttServer.getMqttPort();
        MqttConnectResult result;
        try {
            result = connectFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            connectFuture.cancel(true);
            client.disconnect();
            throw new RuntimeException(String.format("Failed to connect to MQTT server at %s.", hostPort));
        }
        if (!result.isSuccess()) {
            connectFuture.cancel(true);
            client.disconnect();
            throw new RuntimeException(String.format("Failed to connect to MQTT server at %s. Result code is: %s", hostPort, result.getReturnCode()));
        }
        return client;
    }
}
