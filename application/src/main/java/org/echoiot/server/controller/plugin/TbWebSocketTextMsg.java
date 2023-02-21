package org.echoiot.server.controller.plugin;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class TbWebSocketTextMsg implements TbWebSocketMsg<String> {

    @NotNull
    private final String value;

    @NotNull
    @Override
    public TbWebSocketMsgType getType() {
        return TbWebSocketMsgType.TEXT;
    }

    @Override
    public String getMsg() {
        return value;
    }
}
