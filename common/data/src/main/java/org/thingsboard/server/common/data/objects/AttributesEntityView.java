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
@Data
@ApiModel
@NoArgsConstructor
public class AttributesEntityView implements Serializable {

    @ApiModelProperty(position = 1, required = true, value = "List of client-side attribute keys to expose", example = "currentConfiguration")
    private List<String> cs = new ArrayList<>();
    @ApiModelProperty(position = 3, required = true, value = "List of server-side attribute keys to expose", example = "model")
    private List<String> ss = new ArrayList<>();
    @ApiModelProperty(position = 2, required = true, value = "List of shared attribute keys to expose", example = "targetConfiguration")
    private List<String> sh = new ArrayList<>();

    public AttributesEntityView(List<String> cs,
                                List<String> ss,
                                List<String> sh) {

        this.cs = new ArrayList<>(cs);
        this.ss = new ArrayList<>(ss);
        this.sh = new ArrayList<>(sh);
    }

    public AttributesEntityView(AttributesEntityView obj) {
        this(obj.getCs(), obj.getSs(), obj.getSh());
    }
}
