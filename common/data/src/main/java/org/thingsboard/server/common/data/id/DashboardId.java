package org.thingsboard.server.common.data.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.EntityType;

@ApiModel
public class DashboardId extends UUIDBased implements EntityId {

    @JsonCreator
    public DashboardId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DashboardId fromString(String dashboardId) {
        return new DashboardId(UUID.fromString(dashboardId));
    }

    @ApiModelProperty(position = 2, required = true, value = "string", example = "DASHBOARD", allowableValues = "DASHBOARD")
    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }
}
