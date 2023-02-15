package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * @author Andrew Shvayka
 */
@Data
@ApiModel
public class EventInfo extends BaseData<EventId> {

    @ApiModelProperty(position = 1, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @ApiModelProperty(position = 2, value = "Event type", example = "STATS")
    private String type;
    @ApiModelProperty(position = 3, value = "string", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String uid;
    @ApiModelProperty(position = 4, value = "JSON object with Entity Id for which event is created.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId entityId;
    @ApiModelProperty(position = 5, value = "Event body.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode body;

    public EventInfo() {
        super();
    }

    public EventInfo(EventId id) {
        super(id);
    }

    public EventInfo(EventInfo event) {
        super(event);
    }

    @ApiModelProperty(position = 6, value = "Timestamp of the event creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
