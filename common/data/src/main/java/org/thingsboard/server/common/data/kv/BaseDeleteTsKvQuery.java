package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class BaseDeleteTsKvQuery extends BaseTsKvQuery implements DeleteTsKvQuery {

    private final Boolean rewriteLatestIfDeleted;

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs, boolean rewriteLatestIfDeleted) {
        super(key, startTs, endTs);
        this.rewriteLatestIfDeleted = rewriteLatestIfDeleted;
    }

    public BaseDeleteTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, false);
    }

}
