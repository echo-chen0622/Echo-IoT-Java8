package org.echoiot.server.common.data.kv;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class TsKvEntryAggWrapper {

    @NotNull
    private final TsKvEntry entry;
    private final long lastEntryTs;

}
