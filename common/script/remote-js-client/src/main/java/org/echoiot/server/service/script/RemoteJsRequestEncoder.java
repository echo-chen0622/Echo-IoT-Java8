package org.echoiot.server.service.script;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.kafka.TbKafkaEncoder;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Created by Echo on 25.09.18.
 */
public class RemoteJsRequestEncoder implements TbKafkaEncoder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsRequest>> {
    @Override
    public byte[] encode(@NotNull TbProtoQueueMsg<JsInvokeProtos.RemoteJsRequest> value) {
        try {
            return JsonFormat.printer().print(value.getValue()).getBytes(StandardCharsets.UTF_8);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
