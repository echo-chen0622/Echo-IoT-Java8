package org.echoiot.server.transport.coap.efento.adaptor;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.adaptor.JsonConverter;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.efento.CoapEfentoTransportResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class EfentoCoapAdaptor {

    private static final Gson gson = new Gson();

    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, List<CoapEfentoTransportResource.EfentoMeasurements> measurements) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(gson.toJsonTree(measurements));
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert EfentoMeasurements to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }
}
