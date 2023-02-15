package org.thingsboard.server.common.data.kv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.thingsboard.server.common.data.query.TsValue;

/**
 * Represents time series KV data entry
 *
 * @author ashvayka
 *
 */
public interface TsKvEntry extends KvEntry {

    long getTs();

    @JsonIgnore
    int getDataPoints();

    @JsonIgnore
    default TsValue toTsValue() {
        return new TsValue(getTs(), getValueAsString());
    }

}
