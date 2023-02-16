package org.echoiot.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectAttributes implements Serializable {

    private static final long serialVersionUID = 4765123984733721312L;

    private Long dim;
    private String ver;
    private Long pmin;
    private Long pmax;
    private Double gt;
    private Double lt;
    private Double st;

}
