package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@ApiModel
public class ErrorEventFilter implements EventFilter {

    @ApiModelProperty(position = 1, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @ApiModelProperty(position = 2, value = "String value representing the method name when the error happened", example = "onClusterEventMsg")
    protected String method;
    @ApiModelProperty(position = 3, value = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    @Override
    public EventType getEventType() {
        return EventType.ERROR;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(method) || !StringUtils.isEmpty(errorStr);
    }
}
