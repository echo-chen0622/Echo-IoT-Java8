package org.thingsboard.server.common.data.objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
@ApiModel
@Data
@NoArgsConstructor
public class TelemetryEntityView implements Serializable {

    @ApiModelProperty(position = 1, required = true, value = "List of time-series data keys to expose", example = "temperature, humidity")
    private List<String> timeseries;
    @ApiModelProperty(position = 2, required = true, value = "JSON object with attributes to expose")
    private AttributesEntityView attributes;

    public TelemetryEntityView(List<String> timeseries, AttributesEntityView attributes) {

        this.timeseries = new ArrayList<>(timeseries);
        this.attributes = attributes;
    }

    public TelemetryEntityView(TelemetryEntityView obj) {
        this(obj.getTimeseries(), obj.getAttributes());
    }
}
