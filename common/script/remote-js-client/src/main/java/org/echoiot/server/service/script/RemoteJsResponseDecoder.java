package org.echoiot.server.service.script;

import com.google.protobuf.util.JsonFormat;
import org.echoiot.server.queue.TbQueueMsg;
import org.echoiot.server.queue.common.TbProtoQueueMsg;
import org.echoiot.server.queue.kafka.TbKafkaDecoder;
import org.echoiot.server.gen.js.JsInvokeProtos;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Echo on 25.09.18.
 */
public class RemoteJsResponseDecoder implements TbKafkaDecoder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> {

    @NotNull
    @Override
    public TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> decode(@NotNull TbQueueMsg msg) throws IOException {
        JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
        return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
    }
}
