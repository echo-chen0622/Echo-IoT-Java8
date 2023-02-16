package org.echoiot.server.service.transport;

import org.echoiot.server.gen.transport.TransportProtos.ToTransportMsg;

import java.util.function.Consumer;

public interface TbCoreToTransportService {

    void process(String nodeId, ToTransportMsg msg);

    void process(String nodeId, ToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

}
