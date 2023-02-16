package org.echoiot.server.transport.lwm2m.server.client;

import lombok.Data;
import org.echoiot.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultsAddKeyValueProto {
    List<TransportProtos.KeyValueProto> resultAttributes;
    List<TransportProtos.KeyValueProto> resultTelemetries;

    public ResultsAddKeyValueProto() {
        this.resultAttributes = new ArrayList<>();
        this.resultTelemetries = new ArrayList<>();
    }

}
