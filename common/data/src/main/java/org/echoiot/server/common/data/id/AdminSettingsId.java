package org.echoiot.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminSettingsId extends UUIDBased {

    @JsonCreator
    public AdminSettingsId(@JsonProperty("id") UUID id){
        super(id);
    }

}
