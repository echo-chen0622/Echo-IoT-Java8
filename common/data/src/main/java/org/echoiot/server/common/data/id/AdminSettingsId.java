package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class AdminSettingsId extends UUIDBased {

    @JsonCreator
    public AdminSettingsId(@JsonProperty("id") UUID id){
        super(id);
    }

}
