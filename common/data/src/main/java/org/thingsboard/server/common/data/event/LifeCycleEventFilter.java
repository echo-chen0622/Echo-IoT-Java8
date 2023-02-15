package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@ApiModel
public class LifeCycleEventFilter implements EventFilter {

    @ApiModelProperty(position = 1, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @ApiModelProperty(position = 2, value = "String value representing the lifecycle event type", example = "STARTED")
    protected String event;
    @ApiModelProperty(position = 3, value = "String value representing status of the lifecycle event", allowableValues = "Success, Failure")
    protected String status;
    @ApiModelProperty(position = 4, value = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    @Override
    public EventType getEventType() {
        return EventType.LC_EVENT;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(event) || !StringUtils.isEmpty(status) || !StringUtils.isEmpty(errorStr);
    }
}
