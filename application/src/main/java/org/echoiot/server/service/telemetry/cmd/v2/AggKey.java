package org.echoiot.server.service.telemetry.cmd.v2;

import lombok.Data;
import org.echoiot.server.common.data.kv.Aggregation;

@Data
public class AggKey {

    private int id;
    private String key;
    private Aggregation agg;

    private Long previousStartTs;
    private Long previousEndTs;
    private Boolean previousValueOnly;

}
