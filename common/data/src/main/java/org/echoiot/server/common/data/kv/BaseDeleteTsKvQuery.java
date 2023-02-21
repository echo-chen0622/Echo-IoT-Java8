package org.echoiot.server.common.data.kv;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class BaseDeleteTsKvQuery extends BaseTsKvQuery implements DeleteTsKvQuery {

    @NotNull
    private final Boolean rewriteLatestIfDeleted;

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs, boolean rewriteLatestIfDeleted) {
        super(key, startTs, endTs);
        this.rewriteLatestIfDeleted = rewriteLatestIfDeleted;
    }

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, false);
    }

}
