package org.echoiot.server.common.data.kv;

import lombok.ToString;
import org.echoiot.server.common.data.query.TsValue;
import org.jetbrains.annotations.NotNull;

@ToString
public class AggTsKvEntry extends BasicTsKvEntry {

    private static final long serialVersionUID = -1933884317450255935L;

    private final long count;

    public AggTsKvEntry(long ts, KvEntry kv, long count) {
        super(ts, kv);
        this.count = count;
    }

    @NotNull
    @Override
    public TsValue toTsValue() {
        return new TsValue(ts, getValueAsString(), count);
    }
}
