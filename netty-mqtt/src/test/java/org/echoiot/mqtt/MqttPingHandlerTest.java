package org.echoiot.mqtt;

import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MqttPingHandlerTest {

    static final int KEEP_ALIVE_SECONDS = 0;
    static final int PROCESS_SEND_DISCONNECT_MSG_TIME_MS = 500;

    MqttPingHandler mqttPingHandler;

    @BeforeEach
    void setUp() {
        mqttPingHandler = new MqttPingHandler(KEEP_ALIVE_SECONDS);
    }

    @Test
    void givenChannelReaderIdleState_whenNoPingResponse_thenDisconnectClient() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.eventLoop()).thenReturn(new DefaultEventLoop());
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);

        mqttPingHandler.userEventTriggered(ctx, IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
        verify(
                channelFuture,
                after(TimeUnit.SECONDS.toMillis(KEEP_ALIVE_SECONDS) + PROCESS_SEND_DISCONNECT_MSG_TIME_MS)
        ).addListener(eq(ChannelFutureListener.CLOSE));
    }
}
