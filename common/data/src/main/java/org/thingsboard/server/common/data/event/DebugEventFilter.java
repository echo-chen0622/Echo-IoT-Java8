package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.StringUtils;

import java.util.UUID;

@Data
@ApiModel
public abstract class DebugEventFilter implements EventFilter {

    @ApiModelProperty(position = 1, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @ApiModelProperty(position = 10, value = "Boolean value to filter the errors", allowableValues = "false, true")
    protected boolean isError;
    @ApiModelProperty(position = 11, value = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    public void setIsError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || isError || !StringUtils.isEmpty(errorStr);
    }

}
