package org.thingsboard.server.common.data.kv;

public interface ReadTsKvQuery extends TsKvQuery {

    long getInterval();

    int getLimit();

    Aggregation getAggregation();

    String getOrder();

}
