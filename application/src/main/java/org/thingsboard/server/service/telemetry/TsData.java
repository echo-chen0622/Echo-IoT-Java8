package org.thingsboard.server.service.telemetry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class TsData implements Comparable<TsData>{

    private final long ts;
    private final Object value;

    public TsData(long ts, Object value) {
        super();
        this.ts = ts;
        this.value = value;
    }

    @ApiModelProperty(position = 1, value = "Timestamp last updated timeseries, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getTs() {
        return ts;
    }

    @ApiModelProperty(position = 2, value = "Object representing value of timeseries key", example = "20", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(TsData o) {
        return Long.compare(ts, o.ts);
    }

}
