package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.EntityType;

import java.util.UUID;

@ApiModel
public class AlarmId extends UUIDBased implements EntityId {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AlarmId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AlarmId fromString(String alarmId) {
        return new AlarmId(UUID.fromString(alarmId));
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "ALARM", allowableValues = "ALARM")
    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }
}