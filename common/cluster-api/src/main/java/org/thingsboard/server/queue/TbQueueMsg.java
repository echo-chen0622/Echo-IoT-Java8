package org.thingsboard.server.queue;

import java.util.UUID;

public interface TbQueueMsg {

    UUID getKey();

    TbQueueMsgHeaders getHeaders();

    byte[] getData();
}
