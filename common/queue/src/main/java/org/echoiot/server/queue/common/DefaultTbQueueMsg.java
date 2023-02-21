package org.echoiot.server.queue.common;

import lombok.Data;
import org.echoiot.server.queue.TbQueueMsg;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Data
public class DefaultTbQueueMsg implements TbQueueMsg {
    private final UUID key;
    private final byte[] data;
    @NotNull
    private final DefaultTbQueueMsgHeaders headers;

    public DefaultTbQueueMsg(@NotNull TbQueueMsg msg) {
        this.key = msg.getKey();
        this.data = msg.getData();
        @NotNull DefaultTbQueueMsgHeaders headers = new DefaultTbQueueMsgHeaders();
        msg.getHeaders().getData().forEach(headers::put);
        this.headers = headers;
    }

}
