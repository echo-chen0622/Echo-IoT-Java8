package org.thingsboard.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceCredentialsId extends UUIDBased {

    @JsonCreator
    public DeviceCredentialsId(@JsonProperty("id") UUID id) {
        super(id);
    }
}
