package org.thingsboard.server.common.data.kv;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseReadTsKvQuery extends BaseTsKvQuery implements ReadTsKvQuery {

    private final long interval;
    private final int limit;
    private final Aggregation aggregation;
    private final String order;

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation) {
        this(key, startTs, endTs, interval, limit, aggregation, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation, String order) {
        super(key, startTs, endTs);
        this.interval = interval;
        this.limit = limit;
        this.aggregation = aggregation;
        this.order = order;
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, endTs - startTs, 1, Aggregation.AVG, "DESC");
    }

    public BaseReadTsKvQuery(String key, long startTs, long endTs, int limit, String order) {
        this(key, startTs, endTs, endTs - startTs, limit, Aggregation.NONE, order);
    }

    public BaseReadTsKvQuery(ReadTsKvQuery query, long startTs, long endTs) {
        super(query.getId(), query.getKey(), startTs, endTs);
        this.interval = query.getInterval();
        this.limit = query.getLimit();
        this.aggregation = query.getAggregation();
        this.order = query.getOrder();
    }

}
