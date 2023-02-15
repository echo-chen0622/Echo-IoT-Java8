package org.thingsboard.server.service.telemetry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class AttributeData implements Comparable<AttributeData>{

    private final long lastUpdateTs;
    private final String key;
    private final Object value;

    public AttributeData(long lastUpdateTs, String key, Object value) {
        super();
        this.lastUpdateTs = lastUpdateTs;
        this.key = key;
        this.value = value;
    }

    @ApiModelProperty(position = 1, value = "Timestamp last updated attribute, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getLastUpdateTs() {
        return lastUpdateTs;
    }

    @ApiModelProperty(position = 2, value = "String representing attribute key", example = "active", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getKey() {
        return key;
    }

    @ApiModelProperty(position = 3, value = "Object representing value of attribute key", example = "false", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(AttributeData o) {
        return Long.compare(lastUpdateTs, o.lastUpdateTs);
    }

}
