package org.echoiot.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

@ApiModel
@Data
@AllArgsConstructor
public class LwM2mResourceObserve {
    @ApiModelProperty(position = 1, value = "LwM2M Resource Observe id.", example = "0")
    int id;
    @ApiModelProperty(position = 2, value = "LwM2M Resource Observe name.", example = "Data")
    String name;
    @ApiModelProperty(position = 3, value = "LwM2M Resource Observe observe.", example = "false")
    boolean observe;
    @ApiModelProperty(position = 4, value = "LwM2M Resource Observe attribute.", example = "false")
    boolean attribute;
    @ApiModelProperty(position = 5, value = "LwM2M Resource Observe telemetry.", example = "false")
    boolean telemetry;
    @ApiModelProperty(position = 6, value = "LwM2M Resource Observe key name.", example = "data")
    String keyName;

    public LwM2mResourceObserve(int id, String name, boolean observe, boolean attribute, boolean telemetry) {
        this.id = id;
        this.name = name;
        this.observe = observe;
        this.attribute = attribute;
        this.telemetry = telemetry;
        this.keyName = getCamelCase (this.name);
    }

    @NotNull
    private String getCamelCase(String name) {
        name = name.replaceAll("-", " ");
        name = name.replaceAll("_", " ");
        @NotNull String [] nameCamel1 = name.split(" ");
        @NotNull String [] nameCamel2 = new String[nameCamel1.length];
        @NotNull int[] idx = {0 };
        Stream.of(nameCamel1).forEach((s -> {
            nameCamel2[idx[0]] = toProperCase(idx[0]++,  s);
        }));
        return String.join("", nameCamel2);
    }

    @NotNull
    private String toProperCase(int idx, @NotNull String s) {
        if (!s.isEmpty() && s.length()> 0) {
            @NotNull String s1 = (idx == 0) ? s.substring(0, 1).toLowerCase() : s.substring(0, 1).toUpperCase();
            @NotNull String s2 = "";
            if (s.length()> 1) s2 = s.substring(1).toLowerCase();
            s = s1 + s2;
        }
        return s;
    }
}
