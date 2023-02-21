package org.echoiot.server.common.data.kv;

import lombok.Data;
import org.echoiot.server.common.data.query.TsValue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReadTsKvQueryResult {

    private final int queryId;
    // Holds the data list;
    @NotNull
    private final List<TsKvEntry> data;
    // Holds the max ts of the records that match aggregation intervals (not the ts of the aggregation window, but the ts of the last record among all the intervals)
    private final long lastEntryTs;

    @NotNull
    public TsValue[] toTsValues() {
        if (data != null && !data.isEmpty()) {
            @NotNull List<TsValue> queryValues = new ArrayList<>();
            for (@NotNull TsKvEntry v : data) {
                queryValues.add(v.toTsValue()); // TODO: add count here.
            }
            return queryValues.toArray(new TsValue[queryValues.size()]);
        } else {
            return new TsValue[0];
        }
    }

    public TsValue toTsValue(@NotNull ReadTsKvQuery query) {
        if (data == null || data.isEmpty()) {
            if (Aggregation.SUM.equals(query.getAggregation()) || Aggregation.COUNT.equals(query.getAggregation())) {
                long ts = query.getStartTs() + (query.getEndTs() - query.getStartTs()) / 2;
                return new TsValue(ts, "0");
            } else {
                return TsValue.EMPTY;
            }
        }
        if (data.size() > 1) {
            throw new RuntimeException("Query Result has multiple data points!");
        }
        return data.get(0).toTsValue();
    }

}
