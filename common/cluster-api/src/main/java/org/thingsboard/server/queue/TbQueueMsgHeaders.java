package org.thingsboard.server.queue;

import java.util.Map;

public interface TbQueueMsgHeaders {

    byte[] put(String key, byte[] value);

    byte[] get(String key);

    Map<String, byte[]> getData();
}
