package org.echoiot.server.dao.sql;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Data
@Builder
public class TbSqlBlockingQueueParams {

    @NotNull
    private final String logName;
    private final int batchSize;
    private final long maxDelay;
    private final long statsPrintIntervalMs;
    @NotNull
    private final String statsNamePrefix;
    private final boolean batchSortEnabled;
}
