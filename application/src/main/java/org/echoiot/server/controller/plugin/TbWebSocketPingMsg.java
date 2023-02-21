package org.echoiot.server.controller.plugin;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class TbWebSocketPingMsg implements TbWebSocketMsg<ByteBuffer> {

    @NotNull
    public static TbWebSocketPingMsg INSTANCE = new TbWebSocketPingMsg();

    private static final ByteBuffer PING_MSG = ByteBuffer.wrap(new byte[]{});

    @NotNull
    @Override
    public TbWebSocketMsgType getType() {
        return TbWebSocketMsgType.PING;
    }

    @Override
    public ByteBuffer getMsg() {
        return PING_MSG;
    }
}
