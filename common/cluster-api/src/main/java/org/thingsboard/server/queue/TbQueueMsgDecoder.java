package org.thingsboard.server.queue;

import com.google.protobuf.InvalidProtocolBufferException;
import org.thingsboard.server.queue.TbQueueMsg;

public interface TbQueueMsgDecoder<T extends TbQueueMsg> {

    T decode(TbQueueMsg msg) throws InvalidProtocolBufferException;
}
