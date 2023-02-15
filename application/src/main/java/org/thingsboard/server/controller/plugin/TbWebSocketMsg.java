package org.thingsboard.server.controller.plugin;

public interface TbWebSocketMsg<T> {

    TbWebSocketMsgType getType();

    T getMsg();

}
