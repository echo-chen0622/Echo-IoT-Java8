package org.echoiot.server.queue.common;

import org.echoiot.server.queue.TbQueueMsgHeaders;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DefaultTbQueueMsgHeaders implements TbQueueMsgHeaders {

    protected final Map<String, byte[]> data = new HashMap<>();

    @Override
    public byte[] put(String key, byte[] value) {
        return data.put(key, value);
    }

    @Override
    public byte[] get(String key) {
        return data.get(key);
    }

    @NotNull
    @Override
    public Map<String, byte[]> getData() {
        return data;
    }
}
