package org.echoiot.server.queue;

import com.google.protobuf.InvalidProtocolBufferException;

public interface TbQueueMsgDecoder<T extends TbQueueMsg> {

    T decode(TbQueueMsg msg) throws InvalidProtocolBufferException;
}
