package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@ApiModel
public class StatisticsEventFilter implements EventFilter {

    @ApiModelProperty(position = 1, value = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @ApiModelProperty(position = 2, value = "The minimum number of successfully processed messages", example = "25")
    protected Integer minMessagesProcessed;
    @ApiModelProperty(position = 3, value = "The maximum number of successfully processed messages", example = "250")
    protected Integer maxMessagesProcessed;
    @ApiModelProperty(position = 4, value = "The minimum number of errors occurred during messages processing", example = "30")
    protected Integer minErrorsOccurred;
    @ApiModelProperty(position = 5, value = "The maximum number of errors occurred during messages processing", example = "300")
    protected Integer maxErrorsOccurred;

    @Override
    public EventType getEventType() {
        return EventType.STATS;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server)
                || (minMessagesProcessed != null && minMessagesProcessed > 0) || (minErrorsOccurred != null && minErrorsOccurred > 0)
                || (maxMessagesProcessed != null && maxMessagesProcessed > 0) || (maxErrorsOccurred != null && maxErrorsOccurred > 0);
    }
}
