package org.thingsboard.server.queue.common;

import lombok.Data;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;

import java.util.UUID;

@Data
public class TbProtoQueueMsg<T extends com.google.protobuf.GeneratedMessageV3> implements TbQueueMsg {

    private final UUID key;
    protected final T value;
    private final TbQueueMsgHeaders headers;

    public TbProtoQueueMsg(UUID key, T value) {
        this(key, value, new DefaultTbQueueMsgHeaders());
    }

    public TbProtoQueueMsg(UUID key, T value, TbQueueMsgHeaders headers) {
        this.key = key;
        this.value = value;
        this.headers = headers;
    }

    @Override
    public UUID getKey() {
        return key;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return value.toByteArray();
    }
}
