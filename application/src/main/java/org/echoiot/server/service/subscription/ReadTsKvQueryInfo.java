package org.echoiot.server.service.subscription;

import lombok.Data;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.service.telemetry.cmd.v2.AggKey;
import org.jetbrains.annotations.NotNull;

@Data
public class ReadTsKvQueryInfo {

    @NotNull
    private final AggKey key;
    @NotNull
    private final ReadTsKvQuery query;
    private final boolean previous;

}
