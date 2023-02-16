package org.echoiot.server.service.subscription;

import lombok.Data;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.service.telemetry.cmd.v2.AggKey;

@Data
public class ReadTsKvQueryInfo {

    private final AggKey key;
    private final ReadTsKvQuery query;
    private final boolean previous;

}
